package org.rdkit.neo4j.procedures;

/*-
 * #%L
 * RDKit-Neo4j plugin
 * %%
 * Copyright (C) 2019 - 2020 RDKit
 * %%
 * Copyright (C) 2019 Evgeny Sorokin
 * @@ All Rights Reserved @@
 * This file is part of the RDKit Neo4J integration.
 * The contents are covered by the terms of the BSD license
 * which is included in the file LICENSE, found at the root
 * of the neo4j-rdkit source tree.
 * #L%
 */

import lombok.val;
import org.RDKit.RWMol;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.UserFunction;
import org.rdkit.neo4j.utils.Converter;
import org.rdkit.neo4j.utils.RWMolCloseable;

public class UtilProcedures extends BaseProcedure {

  @UserFunction(name = "org.rdkit.utils.svg")
  @Description("RDKit function converts smiles into svg image as text")
  public String createSvg(@Name("smiles") final String smiles) {
    try (val mol = RWMolCloseable.from(RWMol.MolFromSmiles(smiles))) { // todo: add possibility to provide trusted (canonical) smiles
      return Converter.molToSVG(mol);
    }
  }
}
