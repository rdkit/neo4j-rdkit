package org.rdkit.neo4j.procedures;

import static org.rdkit.neo4j.models.NodeFields.CanonicalSmiles;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;
import org.rdkit.fingerprint.FingerprintType;
import org.rdkit.neo4j.models.Constants;
import org.rdkit.neo4j.models.LuceneQuery;
import org.rdkit.neo4j.models.NodeFields;
import org.rdkit.neo4j.utils.Converter;

public class FingerprintProcedures extends BaseProcedure {

  @Procedure(name = "org.rdkit.fingerprint.create", mode = Mode.SCHEMA) // todo: is it SCHEMA or WRITE ? (create index + create property)
  @Description("RDKit create a `fpType` fingerprint and add to all nodes with `labelNames` a property `propertyName` with specified fingerprint. "
      + "Creates a fulltext index on that property. \n"
      + "Possible values for `fpType`: ['morgan', 'topological', 'pattern']. \n"
      + "Restriction for `propertyName`: it must not be equal to rdkit properties of nodes.")
  public void createFingerprintProperty(@Name("label") List<String> labelNames, @Name("fingerprintType") String fpType, @Name("propertyName") String propertyName) throws InterruptedException {
    log.info("Create fingerprint property with parameters: labelsNames=%s, propertyName=%s, fingerprintType=%s", labelNames, propertyName, fpType);

    // start checking parameters
    checkPropertyName(propertyName);

    FingerprintType fingerprintType = FingerprintType.parseString(fpType);
    if (fingerprintType == null) {
      throw new IllegalStateException(String.format("Fingerprint type=%s not found", fpType)); // todo: logically there should be thrown IllegalArgumentException...
    }
    // end checking parameters

    Stream<Node> nodes = getLabeledNodes(labelNames);
    Converter converter = Converter.createConverter(fingerprintType);

    executeBatches(nodes, PAGE_SIZE, node -> {
      final String smiles = (String) node.getProperty(CanonicalSmiles.getValue());
      try {
        final LuceneQuery fp = converter.getLuceneFingerprint(smiles);
        // todo: save fp_type?
        node.setProperty(getPropertyOnes(propertyName), fp.getPositiveBits());
        node.setProperty(getPropertyType(propertyName), fingerprintType.toString());
        node.setProperty(propertyName, fp.getLuceneQuery());
      } catch (Exception e) {
        log.error("Fingerprint type={} unable to convert smiles={}", fpType, smiles);
      }
    });

    final String indexName = propertyName; // todo: how should I name it each time unique, may be use property as a name for this index ?
    createFullTextIndex(indexName, labelNames, Collections.singletonList(propertyName));
  }

  @Procedure(name = "org.rdkit.fingerprint.similarity.smiles", mode = Mode.READ)
  @Description("RDKit similarity search procedure.") // todo: text here
  public Stream<SimilarityResult> similaritySearch(@Name("label") List<String> labelNames, @Name("smiles") String smiles, @Name("fingerprintType") String fpTypeString, @Name("propertyName") String propertyName) {
    String indexName = propertyName;
    checkIndexExistence(labelNames, indexName); // todo: explanation about indexName

    final FingerprintType fpType = FingerprintType.parseString(fpTypeString);
    final Converter converter = Converter.createConverter(fpType);

    LuceneQuery similarityQuery;
    try {
      similarityQuery = converter.getLuceneSimilarityQuery(smiles);
    } catch (RuntimeException e) {
      throw new IllegalArgumentException(String.format("Unable to convert smiles=%s with specified fingerprintType=%s", smiles, fpType));
    }

    /* stream processing objects */
    final String query = similarityQuery.getLuceneQuery();
    final Set<String> queryNumbers = new HashSet<>(Arrays.asList(query.split(similarityQuery.getDelimiter())));
    final long queryPositiveBits = similarityQuery.getPositiveBits();


    final String propertyOnes = getPropertyOnes(propertyName);
    Result result = db.execute("CALL db.index.fulltext.queryNodes($index, $query) "
            + "YIELD node "
            + String.format("RETURN node.canonical_smiles as smiles, %s as fp, %s as fp_ones, node.preferred_name as name, node.luri as luri", "node." + propertyName, "node." + propertyOnes),
        MapUtil.map("index", indexName,
            "query", query)
    );
    return result.stream()
        .parallel()
        .peek(candidate -> {
          long counter = 0;
          for (String position: ((String) candidate.get("fp")).split(Converter.DELIMITER_WHITESPACE)) {
            if (queryNumbers.contains(position)) counter++;
          }

          long candidatePositiveBits = (Long) candidate.get("fp_ones");
          double similarity = 1.0d * counter / (queryPositiveBits + candidatePositiveBits - counter);
          candidate.put("similarity", similarity);
        })
        .map(SimilarityResult::new)
        .sorted((s1, s2) -> Double.compare(s2.similarity, s1.similarity));
  }

  public class SimilarityResult {
    public String name;
    public String luri;
    public String smiles;
    public double similarity;


    public SimilarityResult(Map<String, Object> map) {
      this.luri = (String) map.getOrDefault("luri", null);
      this.name = (String) map.getOrDefault("name", null);
      this.smiles = (String) map.get("smiles");
      this.similarity = (Double) map.get("similarity");
    }
  }

  private void checkPropertyName(final String propertyName) {
    try {
      Constants.from(propertyName);
      throw new IllegalStateException("This property name is protected");
    } catch (IllegalArgumentException e) {
      // no intersection of names found, valueOf thrown an exception
    }

    try {
      NodeFields.from(propertyName);
      throw new IllegalStateException("This property name is protected");
    } catch (IllegalArgumentException e) {
      // no intersection of names found, valueOf thrown an exception
    }
  }

  private String getPropertyOnes(final String propertyName) {
    return propertyName + "_ones";
  }

  private String getPropertyType(final String proprtyName) {
    return proprtyName + "_type";
  }
}
