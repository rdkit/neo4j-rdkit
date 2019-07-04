package org.rdkit.neo4j.eventhandlers;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
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

public class CanonicalSmilesEventHandler implements TransactionEventHandler {


  public static GraphDatabaseService db;
  private static ExecutorService ex;
  private final Label label;

  public CanonicalSmilesEventHandler(GraphDatabaseService graphDatabaseService,
      ExecutorService executor) {
    db = graphDatabaseService;
    ex = executor;
    this.label = Label.label("smiles"); // todo: think about constant ???
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
    val nodes = getNodes(label, data);

    for (Node node : nodes) {
      final Map<String, Object> properties = node.getAllProperties();
      final String smiles = (String) properties.get("smiles"); // todo: may throw exception
      final String canonicalSmiles = Converter.getRDKitSmiles(smiles);
      node.setProperty("canonical_smiles", canonicalSmiles);
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

  private Set<Node> getNodes(Label label, TransactionData data) {
    Set<Node> nodes = StreamSupport.stream(data.createdNodes().spliterator(), false)
        .filter(node -> node.hasLabel(label))
        .collect(Collectors.toSet());

    val labelAssigned = StreamSupport.stream(data.assignedLabels().spliterator(), false)
        .filter(
            labelEntry -> labelEntry.label().equals(label) && !nodes.contains(labelEntry.node()))
        .map(LabelEntry::node)
        .collect(Collectors.toSet());
    nodes.addAll(labelAssigned);

    return nodes;
  }
}
