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
import org.rdkit.neo4j.utils.Converter;
import org.rdkit.neo4j.utils.MolBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CanonicalSmilesEventHandler implements TransactionEventHandler<Object> {

  private static final Logger logger = LoggerFactory.getLogger(CanonicalSmilesEventHandler.class);

  public static GraphDatabaseService db;
  private final Label label;

  public CanonicalSmilesEventHandler(GraphDatabaseService graphDatabaseService) {
    db = graphDatabaseService;
    this.label = Label.label("Chemical"); // todo: think about constant ???
  }

  /* todo: remove comment below
   * The transaction is still open when this method is invoked, making it
   * possible to perform mutating operations in this method. This is however
   * highly discouraged since changes made in this method are not guaranteed to be
   * visible by this or other {@link TransactionEventHandler}s.
   *
   * @param data the changes that will be committed in this transaction.
   * @return a state object (or <code>null</code>) that will be passed on to
   *         {@link #afterCommit(TransactionData, Object)} or
   *         {@link #afterRollback(TransactionData, Object)} of this object.
   * @throws Exception to indicate that the transaction should be rolled back.
   */
  @Override
  public Object beforeCommit(TransactionData data) throws Exception {
    val nodesMol = getNodes(data, label, "mdlmol");

    // todo: catch new nodes with `mol` property and instatiate other properties

    for (Node node: nodesMol) {
      final String mol = (String) node.getProperty("mdlmol");
      final MolBlock block = Converter.convertMolBlock(mol);

      addProperties(node, block);
    }


    val nodesSmiles = getNodes(data, label, "smiles");
    nodesSmiles.removeAll(nodesMol);

    for (Node node: nodesSmiles) {
      final String smiles = (String) node.getProperty("smiles"); // todo: will there appear nodes created only by smiles (not mol file)?
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
    node.setProperty("canonical_smiles", block.getCanonicalSmiles());
    node.setProperty("inchi", block.getInchi());
    node.setProperty("formula", block.getFormula());
    node.setProperty("molecular_weight", block.getMolecularWeight());

    if (!node.hasProperty("mdlmol")) // When molblock is created from smiles
      node.setProperty("mdlmol", block.getMolBlock());
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
