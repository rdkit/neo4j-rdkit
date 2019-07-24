package org.rdkit.neo4j.handlers;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.val;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.event.LabelEntry;
import org.neo4j.graphdb.event.TransactionData;
import org.neo4j.graphdb.event.TransactionEventHandler;
import org.rdkit.fingerprint.DefaultFingerprintFactory;
import org.rdkit.fingerprint.DefaultFingerprintSettings;
import org.rdkit.fingerprint.FingerprintType;
import org.rdkit.neo4j.models.NodeFields;
import org.rdkit.neo4j.utils.Converter;
import org.rdkit.neo4j.models.MolBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RDKitEventHandler implements TransactionEventHandler<Object> {

  private static final Logger logger = LoggerFactory.getLogger(RDKitEventHandler.class);

  public static GraphDatabaseService db;
  private final List<Label> labels;
  private final Converter converter;

  public RDKitEventHandler(GraphDatabaseService graphDatabaseService) {
    db = graphDatabaseService;
    this.labels = Arrays.asList(Label.label(NodeFields.Chemical.getValue()), Label.label(NodeFields.Structure.getValue()));

    // todo: think about injection
    val fpSettings = new DefaultFingerprintSettings(FingerprintType.pattern);
    val fpFactory = new DefaultFingerprintFactory(fpSettings);
    this.converter = new Converter(fpFactory);
  }

  @Override
  public Object beforeCommit(TransactionData data) throws Exception {
    val nodesMol = getNodes(data, NodeFields.MdlMol.getValue());

    for (Node node: nodesMol) {
      final String mol = (String) node.getProperty(NodeFields.MdlMol.getValue());
      final MolBlock block = converter.convertMolBlock(mol);

      addProperties(node, block);
    }


    val nodesSmiles = getNodes(data, NodeFields.Smiles.getValue());
    nodesSmiles.removeAll(nodesMol);

    // todo: will there appear nodes created only by smiles (not mol file)?
    for (Node node: nodesSmiles) {
      final String smiles = (String) node.getProperty(NodeFields.Smiles.getValue());
      final MolBlock block = converter.convertSmiles(smiles);

      addProperties(node, block);
    }

    return data;
  }

  @Override
  public void afterCommit(TransactionData data, Object state) {

  }

  @Override
  public void afterRollback(TransactionData data, Object state) {

  }

  public static void addProperties(final Node node, final MolBlock block) {
    logger.debug("Node={} adding properties: {}", node, block);
    node.setProperty(NodeFields.CanonicalSmiles.getValue(), block.getCanonicalSmiles());
    node.setProperty(NodeFields.Inchi.getValue(), block.getInchi());
    node.setProperty(NodeFields.Formula.getValue(), block.getFormula());
    node.setProperty(NodeFields.MolecularWeight.getValue(), block.getMolecularWeight());
    node.setProperty(NodeFields.FingerprintEncoded.getValue(), block.getFingerprintEncoded());

    // When molblock is created from smiles
    if (!node.hasProperty(NodeFields.MdlMol.getValue()))
      node.setProperty(NodeFields.MdlMol.getValue(), block.getMolBlock());
  }

  /**
   * Return all nodes that obtained specified `labels` and contain a `property` field
   * @param data transaction
   * @param property name
   * @return set of nodes for further update
   */
  private Set<Node> getNodes(final TransactionData data, String property) {
    // todo: logic here needs improvement
    Set<Node> nodes = StreamSupport.stream(data.createdNodes().spliterator(), false)
        .filter(node -> labels.stream().allMatch(node::hasLabel) && node.hasProperty(property))
        .collect(Collectors.toSet());

    val labelAssigned = StreamSupport.stream(data.assignedLabels().spliterator(), false)
        .filter(
            labelEntry -> {
              Node node = labelEntry.node();
              return labels.stream().allMatch(node::hasLabel) && !nodes.contains(node) && node.hasProperty(property);
            })
        .map(LabelEntry::node)
        .collect(Collectors.toSet());
    nodes.addAll(labelAssigned);

    return nodes;
  }
}
