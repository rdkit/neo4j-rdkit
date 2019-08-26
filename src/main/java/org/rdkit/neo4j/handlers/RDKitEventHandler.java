package org.rdkit.neo4j.handlers;

/*-
 * #%L
 * RDKit-Neo4j
 * %%
 * Copyright (C) 2019 RDKit
 * %%
 * Copyright (C) 2019 Evgeny Sorokin
 * @@ All Rights Reserved @@
 * This file is part of the RDKit Neo4J integration.
 * The contents are covered by the terms of the BSD license
 * which is included in the file LICENSE, found at the root
 * of the neo4j-rdkit source tree.
 * #L%
 */

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

import org.rdkit.neo4j.models.Constants;
import org.rdkit.neo4j.models.NodeFields;
import org.rdkit.neo4j.models.NodeParameters;
import org.rdkit.neo4j.utils.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RDKit event handler
 * Handler tracks new nodes with property `smiles` or `mdlmol`
 * In case of new node - creates additional properties, the list of properties may be found in {@link org.rdkit.neo4j.models.NodeFields}
 *
 * todo: add functionality to track new properties during runtime (similarity search can create new properties)
 */
public class RDKitEventHandler implements TransactionEventHandler<Object> {

  private static final Logger logger = LoggerFactory.getLogger(RDKitEventHandler.class);

  public static GraphDatabaseService db;
  private final List<Label> labels;
  private final Converter converter;

  public RDKitEventHandler(GraphDatabaseService graphDatabaseService) {
    db = graphDatabaseService;
    this.labels = Arrays.asList(Label.label(Constants.Chemical.getValue()), Label.label(Constants.Structure.getValue()));
    this.converter = Converter.createDefault();
  }

  /**
   * RDKitEventHandler tracks only before commit changes in db.
   * If suitable item is found - new properties are added
   * Suitable items are found by this method {@link #getNodes(TransactionData, String)}}
   *
   * {@inheritDoc}
   */
  @Override
  public Object beforeCommit(TransactionData data) throws Exception {
    // Obtain nodes with `mdlmol` property
    val nodesMol = getNodes(data, NodeFields.MdlMol.getValue());

    for (Node node: nodesMol) {
      final String mol = (String) node.getProperty(NodeFields.MdlMol.getValue());
      final NodeParameters block = converter.convertMolBlock(mol);

      addProperties(node, block);
    }

    // Obtain nodes with `smiles` property
    val nodesSmiles = getNodes(data, NodeFields.Smiles.getValue());
    nodesSmiles.removeAll(nodesMol);

    for (Node node: nodesSmiles) {
      final String smiles = (String) node.getProperty(NodeFields.Smiles.getValue());
      final NodeParameters block = converter.convertSmiles(smiles);

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

  /**
   * Method updates an object state (Node object) by adding list of properties
   *
   * @param node - to be updated
   * @param block - constructed by rdkit methods list of paramaters to be inserted as property of a node
   */
  public static void addProperties(final Node node, final NodeParameters block) {
    logger.debug("Node={} adding properties: {}", node, block);
    node.setProperty(NodeFields.CanonicalSmiles.getValue(), block.getCanonicalSmiles());
    node.setProperty(NodeFields.Inchi.getValue(), block.getInchi());
    node.setProperty(NodeFields.Formula.getValue(), block.getFormula());
    node.setProperty(NodeFields.MolecularWeight.getValue(), block.getMolecularWeight());
    node.setProperty(NodeFields.FingerprintEncoded.getValue(), block.getFingerprintEncoded());
    node.setProperty(NodeFields.FingerprintOnes.getValue(), block.getFingerpintOnes());

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

    // todo: refactor, get rid of collect and may be stream
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
