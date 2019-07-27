package org.rdkit.neo4j.procedures;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import lombok.val;
import org.RDKit.RWMol;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import org.rdkit.neo4j.models.Constants;
import org.rdkit.neo4j.models.NodeFields;
import org.rdkit.neo4j.models.SSSQuery;
import org.rdkit.neo4j.utils.Converter;
import org.rdkit.neo4j.utils.RWMolCloseable;

public class SubstructureSearch {
  private static final String fingerprintProperty = NodeFields.FingerprintEncoded.getValue();
  private static final String fingerprintOnesProperty = NodeFields.FingerprintOnes.getValue();
  private static final String canonicalSmilesProperty = NodeFields.CanonicalSmiles.getValue();
  private static final String indexName = Constants.IndexName.getValue();
  private static final Converter converter = Converter.createDefault();

  @Context
  public GraphDatabaseService db;

  @Context
  public Log log;


  @Procedure(name = "org.rdkit.search.substructure.createIndex", mode = Mode.SCHEMA)
  @Description("RDKit create a nodeIndex for specific field on top of fingerprint property")
  public void createIndex(@Name("label") List<String> labelNames) {
    log.info("Create whitespace node index on `fp` property");

    Map<String, Object> params = MapUtil.map(
        "index", indexName,
        "labels", labelNames,
        "property", Collections.singletonList(fingerprintProperty)
    );
    db.execute("CALL db.index.fulltext.createNodeIndex($index, $labels, $property, {analyzer: 'whitespace'} )", params);
  }

  @Procedure(name = "org.rdkit.search.substructure.smiles", mode = Mode.READ)
  @Description("RDKit substructure search based on `smiles` property")
  public Stream<NodeSSSResult> substructureSearch(@Name("label") List<String> labelNames, @Name("smiles") String smiles) {
    log.info("Substructure search smiles :: label=%s, smiles=%s", labelNames, smiles);

    // todo: validate smiles is correct (possible)
    checkIndexExistence(labelNames, Constants.IndexName.getValue()); // if index exists, then the values are

    try (final RWMolCloseable query = RWMolCloseable.from(RWMol.MolFromSmiles(smiles))) {
      query.updatePropertyCache();

      final SSSQuery sssQuery = converter.getLuceneFPQuery(query);

      Result result = db.execute("CALL db.index.fulltext.queryNodes($index, $query) "
                  + "YIELD node "
                  + "RETURN node",
          MapUtil.map("index", indexName, "query", sssQuery.getLuceneQuery()));
      val evaluated = result.stream()
          .map(map -> new NodeSSSResult(map, sssQuery))
          .filter(item -> {
            try (RWMolCloseable candidate = RWMolCloseable.from(RWMol.MolFromSmiles(item.canonical_smiles, 0, false))) {
              candidate.updatePropertyCache(false);
              return candidate.hasSubstructMatch(query);
            }
          })
          .sorted(Comparator.comparingLong(n -> n.score))
          .collect(Collectors.toList());
      return evaluated.stream();
    }
  }

  /**
   * Class wraps result of substructure search
   */
  public static class NodeSSSResult {
    public String name;
    public String canonical_smiles;
    public Long score;

    public NodeSSSResult(final Map<String, Object> map, final SSSQuery query) {
      Node node = (Node) map.get("node");
      this.name = (String) node.getProperty("preferred_name", null);
      this.canonical_smiles = (String) node.getProperty(canonicalSmilesProperty);
      long nodeCount = (Long) node.getProperty(fingerprintOnesProperty);
      long queryCount = query.getPositiveBits();
      this.score = nodeCount - queryCount;
    }
  }

  /**
   * Method checks existence of nodeIndex
   * If it does not exist, fulltext query will not be executed (lucene does not contain the data)
   * @param labelNames to query on
   * @param indexName to look for
   */
  private void checkIndexExistence(List<String> labelNames, String indexName) {
    Set<Label> labels = labelNames.stream().map(Label::label).collect(Collectors.toSet());

    try {
      IndexDefinition index = db.schema().getIndexByName(indexName);
      assert index.isNodeIndex();
      assert StreamSupport.stream(index.getLabels().spliterator(), false).allMatch(labels::contains);
    } catch (AssertionError e) {
      log.error("No `{}` node index found", indexName);
      throw e;
    }
  }
}
