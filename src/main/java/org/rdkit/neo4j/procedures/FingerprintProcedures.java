package org.rdkit.neo4j.procedures;

/*-
 * #%L
 * RDKit-Neo4j
 * %%
 * Copyright (C) 2019 RDKit
 * %%
 * Copyright (C) 2019 Evgeny Sorokin
 * @@ All Rights Reserved @@
 * This file is part of the RDKit Neo4J integration.
 * The contents are covered by the terms of the BSD license
 * which is included in the file LICENSE, found at the root
 * of the neo4j-rdkit source tree.
 * #L%
 */

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

/**
 * Class FingerprintProcedures
 * Implements functionality for org.rdkit.fingerprint.* procedures
 * Those procedures allow to create a custom fingerprint property and use similarity search on top of it
 */
public class FingerprintProcedures extends BaseProcedure {

  /**
   * Procedure creates a new property and a fulltext index on top of it.
   * If it is impossible to convert node into specified fingerprint type, it is skipped
   *
   * Creates a `propertyName` property for nodes and two additional supporting properties:  `propertyName` + {"_ones", "_type"}.
   * {@link #getPropertyOnes(String)} {@link #getPropertyType(String)}
   *
   * Those are required to define the type of fingerprint and the amount of positive bits during `similarity` search
   *
   * Created index has name `propertyName` + "_index"
   * {@link #getIndexName(String)}
   *
   * @param labelNames - node labels
   * @param fpType - type of the fingerprint, must exist in {@link NodeFields}
   * @param propertyName - a new property name, which will be created with specified fingerprint
   * @param sanitize
   * @throws InterruptedException if any during the batch task
   */
  @Procedure(name = "org.rdkit.fingerprint.create", mode = Mode.SCHEMA)
  @Description("RDKit create a `fpType` fingerprint and add to all nodes with `labelNames` a property `propertyName` with specified fingerprint. "
      + "Creates a fulltext index on that property. \n"
      + "Possible values for `fpType`: ['morgan', 'topological', 'pattern']. \n"
      + "Restriction for `propertyName`: it must not be equal to rdkit properties of nodes.")
  public void createFingerprintProperty(@Name("label") List<String> labelNames, @Name("fingerprintType") String fpType, @Name("propertyName") String propertyName, @Name(value="sanitize", defaultValue="true") boolean sanitize) throws InterruptedException {
    log.info("Create fingerprint property with parameters: labelsNames=%s, propertyName=%s, fingerprintType=%s", labelNames, propertyName, fpType);

    // START checking parameters
    checkPropertyName(propertyName);

    FingerprintType fingerprintType = FingerprintType.parseString(fpType);
    if (fingerprintType == null) {
      throw new IllegalStateException(String.format("Fingerprint type=%s not found", fpType));
    }
    // END checking parameters

    Stream<Node> nodes = getLabeledNodes(labelNames);
    Converter converter = Converter.createConverter(fingerprintType); // converter of user-requested type

    // Execute batch (may take a long time)
    executeBatches(nodes, PAGE_SIZE, node -> {
      final String smiles = (String) node.getProperty(canonicalSmilesProperty);
      try {
        final LuceneQuery fp = converter.getLuceneFingerprint(smiles, sanitize);
        node.setProperty(getPropertyOnes(propertyName), fp.getPositiveBits());
        node.setProperty(getPropertyType(propertyName), fingerprintType.toString());
        node.setProperty(propertyName, fp.getLuceneQuery());
      } catch (Exception e) { // If node is impossible to convert into specified fingerprint type, it is skipped
        log.error("Fingerprint type=%s unable to convert smiles=%s", fpType, smiles);
      }
    });

    final String propertyIndexName = getIndexName(propertyName);
    createFullTextIndex(propertyIndexName, labelNames, Collections.singletonList(propertyName));

  }

  /**
   * Method implements functionality for similarity search on top of smiles property
   * Convert specified smiles into the requested fingerprint and compare with fingerprints defined by `propertyName`
   *
   * @param labelNames - node labels
   * @param smiles - to be converted into fingerprint and compared
   * @param fpTypeString - type of the fingerprint, must exist in {@link NodeFields}
   * @param propertyName - to be compared with, must exist
   * @param threshold - lower bound of result to be in the result list
   * @param sanitize
   * @return a stream of obtained nodes
   */
  @Procedure(name = "org.rdkit.fingerprint.similarity.smiles", mode = Mode.READ)
  @Description("RDKit similarity search procedure. Finds similarity between provided chemical structure "
      + "(which is created of type=`fingerprintType`, from `smiles`) and "
      + "fingerprints placed under proprty=`propertyName`. Values below `threshold` are discarded.")
  public Stream<SimilarityResult> similaritySearch(@Name("label") List<String> labelNames,
                                                   @Name("smiles") String smiles,
                                                   @Name("fingerprintType") String fpTypeString,
                                                   @Name("propertyName") String propertyName,
                                                   @Name("threshold") Double threshold,
                                                   @Name(value="sanitize", defaultValue="true") boolean sanitize) {
    log.info("Call similaritySearch labelNames=%s, smiles=%s, fptype=%s, propertyName=%s, threshold=%s", labelNames, smiles, fpTypeString, propertyName, threshold);
    String indexName = getIndexName(propertyName);

    // START param check
    checkIndexExistence(labelNames, indexName);
    checkThreshold(threshold);

    final FingerprintType fpType = FingerprintType.parseString(fpTypeString);
    final Converter converter = Converter.createConverter(fpType);

    LuceneQuery similarityQuery;
    try {
      similarityQuery = converter.getLuceneSimilarityQuery(smiles, sanitize);
    } catch (RuntimeException e) {
      throw new IllegalArgumentException(String.format("Unable to convert smiles=%s with specified fingerprintType=%s", smiles, fpType));
    }

    // END param check

    /* stream processing objects */
    final String query = similarityQuery.getLuceneQuery();
    final Set<String> queryNumbers = new HashSet<>(Arrays.asList(query.split(similarityQuery.getDelimiter())));
    final long queryPositiveBits = similarityQuery.getPositiveBits();
    final String propertyOnes = getPropertyOnes(propertyName);

    Result result = db.execute("CALL db.index.fulltext.queryNodes($index, $query) "
            + "YIELD node "
            + String.format("RETURN node.canonical_smiles as smiles, %s as fp, %s as fp_ones, node.preferred_name as name, node.luri as luri",
                "node." + propertyName, "node." + propertyOnes), // todo: looks bad
        MapUtil.map("index", indexName, "query", query));

    // Process the stream, get all nodes which contain at least one bit position from query object
    return result.stream()
        .peek(candidate -> {
          long counter = 0;
          for (String position: ((String) candidate.get("fp")).split(Converter.DELIMITER_WHITESPACE)) { // todo: refactor
            if (queryNumbers.contains(position)) counter++;
          }

          long candidatePositiveBits = (Long) candidate.get("fp_ones");
          double similarity = 1.0d * counter / (queryPositiveBits + candidatePositiveBits - counter);
          candidate.put("similarity", similarity);
        })
//        .parallel()
        .filter(map -> (Double) map.get("similarity") > threshold)
        .map(SimilarityResult::new)
        .sorted((s1, s2) -> Double.compare(s2.similarity, s1.similarity));
  }

  /**
   * Similarity result wrapper
   */
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

  /**
   * Method checks passed parameter `threshold`
   * @param threshold to be checked
   */
  private void checkThreshold(Double threshold) {
    if (threshold == null || threshold <= 0.0d || threshold > 1.0d)
      throw new IllegalStateException(String.format("Threshold value incorrect, value=%f", threshold));
  }

  /**
   * Method checks passed parameter `propertyName` for similarity.create procedure
   * Property name must not be protected, as it would break the logic
   *
   * @param propertyName to be chedked
   */
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

  /**
   * Name convention for similarity fingerprint ones
   */
  private String getPropertyOnes(final String propertyName) {
    return propertyName + "_ones";
  }

  /**
   * Name convention for similarity fingerprint type
   */
  private String getPropertyType(final String propertyName) {
    return propertyName + "_type";
  }

  /**
   * Name convention for similarity fingerprint index
   */
  private String getIndexName(final String propertyName) {
    return propertyName + "_index";
  }
}
