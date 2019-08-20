package org.rdkit.neo4j.procedures;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.helpers.collection.PagingIterator;
import org.neo4j.logging.Log;
import org.neo4j.procedure.Context;
import org.rdkit.neo4j.models.Constants;
import org.rdkit.neo4j.models.NodeFields;

public abstract class BaseProcedure {
  static final String fingerprintProperty = NodeFields.FingerprintEncoded.getValue();
  static final String fingerprintOnesProperty = NodeFields.FingerprintOnes.getValue();
  static final String canonicalSmilesProperty = NodeFields.CanonicalSmiles.getValue();
  static final String indexName = Constants.IndexName.getValue();

  static final int PAGE_SIZE = 10_000;

  @Context
  public GraphDatabaseService db;

  @Context
  public Log log;

  /**
   * Method checks existence of nodeIndex
   * If it does not exist, fulltext query will not be executed (lucene does not contain the data)
   * @param labelNames to query on
   * @param indexName to look for
   */
  void checkIndexExistence(List<String> labelNames, String indexName) {
    Set<Label> labels = labelNames.stream().map(Label::label).collect(Collectors.toSet());

    try {
      IndexDefinition index = db.schema().getIndexByName(indexName);
      assert index.isNodeIndex();
      assert StreamSupport.stream(index.getLabels().spliterator(), false).allMatch(labels::contains);
    } catch (AssertionError e) {
      log.error("No `%s` node index found", indexName);
      throw e;
    }
  }

  void createFullTextIndex(final String indexName, final List<String> labelNames, final List<String> properties) {
    Map<String, Object> params = MapUtil.map(
        "index", indexName,
        "labels", labelNames,
        "property", properties
    );

    db.execute("CALL db.index.fulltext.createNodeIndex($index, $labels, $property, {analyzer: 'whitespace'} )", params);
  }

  /**
   * Method returns nodes with specified labels
   * @param labelNames list
   * @return stream of nodes
   */
  Stream<Node> getLabeledNodes(List<String> labelNames) {
    final String firstLabel = labelNames.get(0);
    final List<Label> labels = labelNames.stream().map(Label::label).collect(Collectors.toList());

    return db.findNodes(Label.label(firstLabel))
        .stream()
//        .parallel()
        .filter(node -> labels.stream().allMatch(node::hasLabel));
  }


  // todo: requires great explanation
  // todo: requires refactor (speed up)
  void executeBatches(final Stream<Node> nodes, final int batchSize, Consumer<? super Node> nodeAction) throws InterruptedException {
    Iterator<Node> nodeIterator = nodes.iterator();
    final PagingIterator<Node> pagingIterator = new PagingIterator<>(nodeIterator, batchSize);

    Thread t = new Thread(() -> {  // we do explicit tx management so we require a separate thread

      Transaction tx = db.beginTx();  // tx needs to opened here - we need one upon consuming the iterator
      try {

        int numberOfBatches = 0;
        while (pagingIterator.hasNext()) {
          Iterator<Node> page = pagingIterator.nextPage();

          tx.success();
          tx.close();
          tx = db.beginTx();

          page.forEachRemaining(nodeAction);
          numberOfBatches++;
          log.info("batch # %d", numberOfBatches);
        }
        log.info("done, ran %d batches successfully", numberOfBatches);


      } finally {
        if (tx!=null) {
          tx.success();
          tx.close();
        }
      }

    });
    t.start();
    t.join();
  }
}
