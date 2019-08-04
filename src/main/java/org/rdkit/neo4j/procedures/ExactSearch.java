package org.rdkit.neo4j.procedures;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.neo4j.graphdb.*;
import org.neo4j.helpers.collection.PagingIterator;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import org.rdkit.neo4j.handlers.RDKitEventHandler;
import org.rdkit.neo4j.models.Constants;
import org.rdkit.neo4j.models.MolBlock;
import org.rdkit.neo4j.models.NodeFields;
import org.rdkit.neo4j.utils.Converter;

public class ExactSearch {
  private static final String query = "MATCH ($nodelabels { $property: $value }) RETURN node";
  private static final int PAGE_SIZE = 10_000;
  private static final Converter converter = Converter.createDefault();

  @Context
  public GraphDatabaseService db;

  @Context
  public Log log;


  @Procedure(name = "org.rdkit.search.exact.smiles", mode = Mode.READ)
  @Description("RDKit exact search on `smiles` property")
  public Stream<NodeWrapper> exactSearchSmiles(@Name("label") List<String> labelNames, @Name("smiles") String smiles) {
    log.info("Exact search smiles :: label=%s, smiles=%s", labelNames, smiles);

    // todo: add index on canonical_smiles property
    final String rdkitSmiles = converter.getRDKitSmiles(smiles);
    return findLabeledNodes(labelNames, NodeFields.CanonicalSmiles.getValue(), rdkitSmiles);
  }

  @Procedure(name = "org.rdkit.search.exact.mol", mode = Mode.READ)
  @Description("RDKit exact search on `mdlmol` property")
  public Stream<NodeWrapper> exactSearchMol(@Name("labels") List<String> labelNames, @Name("mol") String molBlock) {
    log.info("Exact search mol :: label=%s, molBlock=%s", labelNames, molBlock);

    final String rdkitSmiles = converter.convertMolBlock(molBlock).getCanonicalSmiles();
    return findLabeledNodes(labelNames, NodeFields.CanonicalSmiles.getValue(), rdkitSmiles);
  }

  @Procedure(name = "org.rdkit.update", mode = Mode.WRITE)
  @Description("RDKit update procedure, allows to construct ['formula', 'molecular_weight', 'canonical_smiles'] values from 'mdlmol' property")
  public Stream<NodeWrapper> createPropertiesMol(@Name("labels") List<String> labelNames) throws InterruptedException {
    log.info("Update nodes with labels=%s, create additional fields", labelNames);

    final String firstLabel = labelNames.get(0);
    final List<Label> labels = labelNames.stream().map(Label::label).collect(Collectors.toList());

    Iterator<Node> nodeIterator = db.findNodes(Label.label(firstLabel))
            .stream()
            .filter(node -> labels.stream().allMatch(node::hasLabel))
            .iterator();

    final PagingIterator<Node> pagingIterator = new PagingIterator<>(nodeIterator, PAGE_SIZE);

    // todo: refactor
    Thread t = new Thread(() -> {  // we do explicit tx management so we require a separate thread

      Transaction tx = db.beginTx();  // tx needs to opened here - we need one upon consuming the iterator
      try {

        int numberOfBatches = 0;
        while (pagingIterator.hasNext()) {
          Iterator<Node> page = pagingIterator.nextPage();

          tx.success();
          tx.close();
          tx = db.beginTx();

          page.forEachRemaining(node -> {
            final String mol = (String) node.getProperty("mdlmol");
            final MolBlock block = converter.convertMolBlock(mol);
            RDKitEventHandler.addProperties(node, block);

          });
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
    return Stream.empty();
  }

  public static class NodeWrapper {

    public Node node;

    public NodeWrapper(Node node) {
      this.node = node;
    }
  }

  private Stream<NodeWrapper> findLabeledNodes(List<String> labelNames, String property, String value) {
    final String firstLabel = Constants.Chemical.getValue();
    final List<Label> labels = labelNames.stream().map(Label::label).collect(Collectors.toList());

    return db.findNodes(Label.label(firstLabel), property, value)
        .stream()
        .filter(node -> labels.stream().allMatch(node::hasLabel))
        .map(NodeWrapper::new);
  }
}
