package org.rdkit.neo4j.procedures;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import lombok.val;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import org.rdkit.neo4j.models.Constants;
import org.rdkit.neo4j.models.NodeFields;
import org.rdkit.neo4j.models.SSSQuery;
import org.rdkit.neo4j.utils.Converter;

public class SubstructureSearch {
  private static final String createIndexQuery = "CALL db.index.fulltext.createNodeIndex('%s', [%s], ['%s'], {analyzer: 'whitespace'} )";
  private static final String fingerprintProperty = NodeFields.FingerprintEncoded.getValue();
  private static final String fingerprintOnesProperty = "node." + NodeFields.FingerprintOnes.getValue();
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

    val labelsEscaped = labelNames.stream().map(name -> '\'' + name + '\'').collect(Collectors.toList());
    val labelsDelimited = String.join(", ", labelsEscaped);

    String indexQuery = String.format(createIndexQuery, indexName, labelsDelimited, fingerprintProperty);
//    String indexAwait = String.format("CALL db.index.fulltext.awaitIndex('%s', 120)", indexName);
    db.execute(indexQuery);
//    db.execute(indexAwait);
  }

  @Procedure(name = "org.rdkit.search.substructure.smiles", mode = Mode.READ)
  @Description("RDKit substructure search based on `smiles` property")
  public Stream<NodeSSSResult> substructureSearch(@Name("label") List<String> labelNames, @Name("smiles") String smiles) {
    log.info("Substructure search smiles :: label=%s, smiles=%s", labelNames, smiles);

    // todo: validate smiles is correct (possible)
    checkIndexExistence(labelNames, Constants.IndexName.getValue()); // if index exists, then the values are
    final SSSQuery query = converter.getLuceneFPQuery(smiles);

    Result result = db.execute(String.format("CALL db.index.fulltext.queryNodes('%s', $query) YIELD node RETURN node.preferred_name, node.canonical_smiles, %s", indexName, fingerprintOnesProperty), // todo: a const is used here
      MapUtil.map("query", query.getLuceneQuery()));
    return result.stream().map(map -> new NodeSSSResult(map, query)).sorted(Comparator.comparingLong(n -> n.score));
  }

  /**
   * Class wraps result of substructure search
   */
  public static class NodeSSSResult {
    public String name;
    public String canonical_smiles;
    public Long score;

    public NodeSSSResult(final Map<String, Object> map, final SSSQuery query) {
      this.name = (String) map.get("node.preferred_name");
      this.canonical_smiles = (String) map.get("node.canonical_smiles");
      long nodeCount = (Long) map.get(fingerprintOnesProperty);
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
