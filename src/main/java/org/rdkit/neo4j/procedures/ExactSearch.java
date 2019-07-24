package org.rdkit.neo4j.procedures;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.val;
import org.neo4j.graphdb.*;
import org.neo4j.helpers.collection.PagingIterator;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import org.rdkit.fingerprint.DefaultFingerprintFactory;
import org.rdkit.fingerprint.DefaultFingerprintSettings;
import org.rdkit.fingerprint.FingerprintType;
import org.rdkit.neo4j.handlers.RDKitEventHandler;
import org.rdkit.neo4j.models.MolBlock;
import org.rdkit.neo4j.models.NodeFields;
import org.rdkit.neo4j.utils.Converter;

public class ExactSearch {
  private static final String query = "MATCH (node:%s { %s: '%s' }) RETURN node";
  private final int PAGE_SIZE = 10_000;

  @Context
  public GraphDatabaseService db;

  @Context
  public Log log;

  private final Converter converter;

  public ExactSearch() {
    // todo: think about injection
    val fpSettings = new DefaultFingerprintSettings(FingerprintType.pattern);
    val fpFactory = new DefaultFingerprintFactory(fpSettings);
    this.converter = new Converter(fpFactory);
  }

  @Procedure(name = "org.rdkit.search.exact.smiles", mode = Mode.READ)
  @Description("RDKit exact search on `smiles` property")
  public Stream<NodeWrapper> exactSearchSmiles(@Name("label") List<String> labelNames, @Name("smiles") String smiles) {
    log.info("Exact search smiles :: label={}, smiles={}", labelNames, smiles);

    // todo: validate smiles is correct (possible)

    final String rdkitSmiles = converter.getRDKitSmiles(smiles);
    final String labels = String.join(":", labelNames);

    Result result = db.execute(String.format(query, labels, NodeFields.CanonicalSmiles.getValue(), rdkitSmiles));
    return result.stream().map(NodeWrapper::new);
  }

  @Procedure(name = "org.rdkit.search.exact.mol", mode = Mode.READ)
  @Description("RDKit exact search on `mdlmol` property")
  public Stream<NodeWrapper> exactSearchMol(@Name("labels") List<String> labelNames, @Name("mol") String molBlock) {
    log.info("Exact search mol :: label={}, molBlock={}", labelNames, molBlock);

    final String rdkitSmiles = converter.convertMolBlock(molBlock).getCanonicalSmiles();
    final String labels = String.join(":", labelNames);

    Result result = db.execute(String.format(query, labels, NodeFields.CanonicalSmiles.getValue(), rdkitSmiles));
    return result.stream().map(NodeWrapper::new);
  }

  @Procedure(name = "org.rdkit.update", mode = Mode.WRITE)
  @Description("RDKit update procedure, allows to construct ['formula', 'molecular_weight', 'canonical_smiles'] values from 'mdlmol' property")
  public Stream<NodeWrapper> createPropertiesMol(@Name("labels") List<String> labelNames) throws InterruptedException {
    log.info("Update nodes with labels={}, create additional fields", labelNames);

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

  static class NodeWrapper {

    public Node node;

    NodeWrapper(Node node) {
      this.node = node;
    }

    NodeWrapper(Map<String, Object> map) {
      this((Node) map.get("node"));
    }
  }
}
