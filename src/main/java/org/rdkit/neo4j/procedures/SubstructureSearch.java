package org.rdkit.neo4j.procedures;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import lombok.val;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.logging.Log;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import org.rdkit.fingerprint.DefaultFingerprintFactory;
import org.rdkit.fingerprint.DefaultFingerprintSettings;
import org.rdkit.fingerprint.FingerprintType;
import org.rdkit.neo4j.models.NodeFields;
import org.rdkit.neo4j.models.SSSQuery;
import org.rdkit.neo4j.utils.Converter;

public class SubstructureSearch {
  @Context
  public GraphDatabaseService db;

  @Context
  public Log log;

  private final Converter converter;

  public SubstructureSearch() {
    // todo: think about injection
    val fpSettings = new DefaultFingerprintSettings(FingerprintType.pattern);
    val fpFactory = new DefaultFingerprintFactory(fpSettings);
    this.converter = new Converter(fpFactory);
  }

  @Procedure(name = "org.rdkit.search.substructure.smiles", mode = Mode.READ)
  @Description("RDKit substructure search based on `smiles` property")
  public Stream<NodeSSSResult> substructureSearch(@Name("label") List<String> labelNames, @Name("smiles") String smiles, @Name("indexName") String indexName) {
    log.info("Substructure search smiles :: label={}, smiles={}", labelNames, smiles);

    // todo: validate smiles is correct (possible)

    checkIndexExistence(labelNames, indexName); // if index exists, then the values are

    final SSSQuery query = converter.getLuceneFPQuery(smiles);
    Result result = db.execute("CALL db.index.fulltext.queryNodes('bitset', $query) YIELD node " +
        "RETURN node, node.$fpOnes",
      MapUtil.map("query", query.getLuceneQuery(), "fpOnes", NodeFields.FingerprintOnes.getValue()));
    return result.stream().map(map -> new NodeSSSResult(map, query)).sorted(Comparator.comparingInt(n -> n.score));
  }

  /**
   * Class wraps result of substructure search
   */
  static class NodeSSSResult {
    public Node node;
    public int score; // todo: add explanation

    NodeSSSResult(final Map<String, Object> map, final SSSQuery query) {
      this.node = (Node) map.get("node");
      int nodeCount = (Integer) map.get(NodeFields.FingerprintOnes.getValue());
      int queryCount = query.getPositiveBits();
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
