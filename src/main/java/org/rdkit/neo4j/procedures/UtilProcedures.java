package org.rdkit.neo4j.procedures;

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
