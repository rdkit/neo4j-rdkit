package org.rdkit.neo4j.procedures;

import java.util.List;
import java.util.stream.Stream;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.logging.Log;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;
import org.rdkit.lucene.LuceneWrapper;
import org.rdkit.lucene.LuceneWrapperProvider;
import org.rdkit.neo4j.models.NodeFields;
import org.rdkit.neo4j.procedures.ExactSearch.NodeWrapper;
import org.rdkit.neo4j.utils.Converter;

public class SubstructureSearch {
  @Context
  public GraphDatabaseService db;

  @Context
  public Log log;


  @Procedure(name = "org.rdkit.search.substructure.smiles", mode = Mode.READ)
  @Description("RDKit substructure search based on `smiles` property")
  public Stream<NodeWrapper> substructureSearch(@Name("label") List<String> labelNames, @Name("smiles") String smiles) {
    log.info("Substructure search smiles :: label={}, smiles={}", labelNames, smiles);

    // todo: validate smiles is correct (possible)

    final String rdkitSmiles = Converter.getRDKitSmiles(smiles);
    final String labels = String.join(":", labelNames);

    Result result = null; // todo: IMPLEMENT
    return result.stream().map(NodeWrapper::new);
  }

  @Procedure(name = "org.rdkit.lucene.update", mode = Mode.WRITE)
  @Description("RDKit substructure search based on `smiles` property")
  public Stream<NodeWrapper> updateLuceneIndex(@Name("label") List<String> labelNames) {
    log.info("Lucene update index :: label={}", labelNames);

    LuceneWrapper wrapper = LuceneWrapperProvider.createDefaultLuceneWrapper();

  }
}
