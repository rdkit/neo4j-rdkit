package org.rdkit.neo4j.procedures;

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

import org.RDKit.MolSanitizeException;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.kernel.configuration.Config;
import org.neo4j.kernel.internal.GraphDatabaseAPI;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;
import org.rdkit.neo4j.config.RDKitSettings;
import org.rdkit.neo4j.handlers.RDKitEventHandler;
import org.rdkit.neo4j.models.Constants;
import org.rdkit.neo4j.models.NodeFields;
import org.rdkit.neo4j.models.NodeParameters;
import org.rdkit.neo4j.utils.Converter;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * ExactSearch class
 * Implements functionality for org.rdkit.search.exact.* procedures
 */
public class ExactSearch extends BaseProcedure {
  private static final Converter converter = Converter.createDefault();

  /**
   * Method executes exact search by `smiles` value
   * Canonicalizes into rdkit form provided `smiles` and finds exact match in the db
   *
   * @param labelNames - node labels
   * @param smiles - to be found
   * @return obtained node
   */
  @Procedure(name = "org.rdkit.search.exact.smiles", mode = Mode.READ)
  @Description("RDKit exact search on `smiles` property")
  public Stream<NodeWrapper> exactSearchSmiles(@Name("label") List<String> labelNames, @Name("smiles") String smiles) {
    log.info("Exact search smiles :: label=%s, smiles=%s", labelNames, smiles);

    final String rdkitSmiles = converter.getRDKitSmiles(smiles);
    return findLabeledNodes(labelNames, NodeFields.CanonicalSmiles.getValue(), rdkitSmiles);
  }

  /**
   * Method executes exact search by `mdlmol` property
   * In reality it transforms `mdlmol` value into rdkit canonicalized smiles and executes method above
   *
   * @param labelNames - node labels
   * @param molBlock - mdlmol block value
   * @return obtained node
   */
  @Procedure(name = "org.rdkit.search.exact.mol", mode = Mode.READ)
  @Description("RDKit exact search on `mdlmol` property")
  public Stream<NodeWrapper> exactSearchMol(@Name("labels") List<String> labelNames, @Name("mol") String molBlock) {
    log.info("Exact search mol :: label=%s, molBlock=%s", labelNames, molBlock);

    Config config = ((GraphDatabaseAPI) db).getDependencyResolver().resolveDependency(Config.class);
    boolean sanitize = config.get(RDKitSettings.indexSanitize);

    NodeParameters nodeParameters;
    try {
      nodeParameters = converter.convertMolBlock(molBlock, true);

    } catch (MolSanitizeException e) {
      if (sanitize) {
        throw e;
      } else  {
        nodeParameters = converter.convertMolBlock(molBlock, false);
      }
    }

    final String rdkitSmiles = nodeParameters.getCanonicalSmiles();
    return findLabeledNodes(labelNames, NodeFields.CanonicalSmiles.getValue(), rdkitSmiles);


  }

  /**
   * Method creates properties for the nodes with `mdlmol`
   * If the database is created without plugin, the node properties should be created manually by this procedure
   *
   * @param labelNames - node labels
   * @return an empty stream
   * @throws InterruptedException if batch task is interrupted
   */
  @Procedure(name = "org.rdkit.update", mode = Mode.WRITE)
  @Description("RDKit update procedure, allows to construct ['formula', 'molecular_weight', 'canonical_smiles'] values from 'mdlmol' property")
  public Stream<NodeWrapper> createProperties(@Name("labels") List<String> labelNames, @Name(value = "sanitize", defaultValue = "true") boolean sanitize) throws InterruptedException {
    log.info("Update nodes with labels=%s, create additional fields", labelNames);
    // todo: add functionality to skip nodes that already have required properties
    executeBatches(getLabeledNodes(labelNames), PAGE_SIZE, node -> {
      final String mol = (String) node.getProperty("mdlmol");
      try {
        final NodeParameters block = converter.convertMolBlock(mol, sanitize);
        RDKitEventHandler.addProperties(node, block);
      } catch (Exception e) {
        final String luri = (String) node.getProperty("luri", "<undefined>");
        log.error("Unable to convert node with luri={}", luri);
      }
    });
    return Stream.empty();
  }

  /**
   * Class result wrapper for exact search
   */
  public static class NodeWrapper {

    public String name;
    public String luri;
    public String canonical_smiles;

    public NodeWrapper(Node node) {
      this.canonical_smiles = (String) node.getProperty(NodeFields.CanonicalSmiles.getValue());
      this.name = (String) node.getProperty("preferred_name", null);
      this.luri = (String) node.getProperty("luri", null);
    }
  }

  /**
   * Method finds nodes with specified labels and specified property
   * @param labelNames
   * @param property
   * @param value
   * @return
   */
  private Stream<NodeWrapper> findLabeledNodes(List<String> labelNames, String property, String value) {
    final String firstLabel = Constants.Chemical.getValue();
    final List<Label> labels = labelNames.stream().map(Label::label).collect(Collectors.toList());

    return db.findNodes(Label.label(firstLabel), property, value)
        .stream()
//        .parallel()
        .filter(node -> labels.stream().allMatch(node::hasLabel))
        .map(NodeWrapper::new);
  }
}
