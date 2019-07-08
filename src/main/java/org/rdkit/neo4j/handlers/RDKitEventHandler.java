package org.rdkit.neo4j.handlers;

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
import org.rdkit.neo4j.models.NodeFields;
import org.rdkit.neo4j.utils.Converter;
import org.rdkit.neo4j.models.MolBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RDKitEventHandler implements TransactionEventHandler<Object> {

  private static final Logger logger = LoggerFactory.getLogger(RDKitEventHandler.class);

  public static GraphDatabaseService db;
  private final Label label;

  public RDKitEventHandler(GraphDatabaseService graphDatabaseService) {
    db = graphDatabaseService;
    this.label = Label.label(NodeFields.Label.getValue());
  }

  @Override
  public Object beforeCommit(TransactionData data) throws Exception {
    val nodesMol = getNodes(data, label, NodeFields.MdlMol.getValue());

    // todo: catch new nodes with `mol` property and instatiate other properties

    for (Node node: nodesMol) {
      final String mol = (String) node.getProperty(NodeFields.MdlMol.getValue());
      final MolBlock block = Converter.convertMolBlock(mol);

      addProperties(node, block);
    }


    val nodesSmiles = getNodes(data, label, NodeFields.Smiles.getValue());
    nodesSmiles.removeAll(nodesMol);

    // todo: will there appear nodes created only by smiles (not mol file)?
    for (Node node: nodesSmiles) {
      final String smiles = (String) node.getProperty(NodeFields.Smiles.getValue());
      final MolBlock block = Converter.convertSmiles(smiles);

      addProperties(node, block);
    }

    return data;
  }

  @Override
  public void afterCommit(TransactionData data, Object state) {
//    val nodes = getNodes(label, data);
//    boolean allNodesMatch = nodes.stream().
//        allMatch(node -> node.getProperty("canonical_smiles", null) != null);
  }

  @Override
  public void afterRollback(TransactionData data, Object state) {

  }

  private void addProperties(final Node node, final MolBlock block) {
    logger.debug("Node={} adding properties: {}", node, block);
    node.setProperty(NodeFields.CanonicalSmiles.getValue(), block.getCanonicalSmiles());
    node.setProperty(NodeFields.Inchi.getValue(), block.getInchi());
    node.setProperty(NodeFields.Formula.getValue(), block.getFormula());
    node.setProperty(NodeFields.MolecularWeight.getValue(), block.getMolecularWeight());

    // When molblock is created from smiles
    if (!node.hasProperty(NodeFields.MdlMol.getValue()))
      node.setProperty(NodeFields.MdlMol.getValue(), block.getMolBlock());
  }

  private Set<Node> getNodes(final TransactionData data, Label label, String property) {
    // todo: logic here needs improvement
    Set<Node> nodes = StreamSupport.stream(data.createdNodes().spliterator(), false)
        .filter(node -> node.hasLabel(label) && node.hasProperty(property))
        .collect(Collectors.toSet());

    val labelAssigned = StreamSupport.stream(data.assignedLabels().spliterator(), false)
        .filter(
            labelEntry -> {
              Node n = labelEntry.node();
              Label l = labelEntry.label();
              return l.equals(label) && !nodes.contains(n) && n.hasProperty(property);
            })
        .map(LabelEntry::node)
        .collect(Collectors.toSet());
    nodes.addAll(labelAssigned);

    return nodes;
  }
}
