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

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.neo4j.graphdb.*;
import org.neo4j.procedure.*;

import org.rdkit.neo4j.handlers.RDKitEventHandler;
import org.rdkit.neo4j.models.Constants;
import org.rdkit.neo4j.models.NodeParameters;
import org.rdkit.neo4j.models.NodeFields;
import org.rdkit.neo4j.utils.Converter;

public class ExactSearch extends BaseProcedure {
  private static final Converter converter = Converter.createDefault();

  @Procedure(name = "org.rdkit.search.exact.smiles", mode = Mode.READ)
  @Description("RDKit exact search on `smiles` property")
  public Stream<NodeWrapper> exactSearchSmiles(@Name("label") List<String> labelNames, @Name("smiles") String smiles) {
    log.info("Exact search smiles :: label=%s, smiles=%s", labelNames, smiles);

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

    executeBatches(getLabeledNodes(labelNames), PAGE_SIZE, node -> {
      final String mol = (String) node.getProperty("mdlmol");
      try {
        final NodeParameters block = converter.convertMolBlock(mol);
        RDKitEventHandler.addProperties(node, block);
      } catch (Exception e) {
        final String luri = (String) node.getProperty("luri", "<undefined>");
        log.error("Unable to convert node with luri={}", luri);
      }
    });
    return Stream.empty();
  }

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
