package org.neo4j.kernel.api.impl.fulltext.analyzer.providers;

import org.apache.lucene.analysis.Analyzer;
import org.neo4j.graphdb.index.fulltext.AnalyzerProvider;
import org.neo4j.helpers.Service;
import org.rdkit.neo4j.analyzer.RDKitAnalyzer;

@Service.Implementation( AnalyzerProvider.class )
public class RDKit extends AnalyzerProvider {

//  todo: * The {@code jar} that includes this implementation must also contain a {@code META-INF/services/org.neo4j.graphdb.index.fulltext.AnalyzerProvider} file,
//  todo: * that contains the fully-qualified class names of all of the {@code AnalyzerProvider} implementations it contains.

  public RDKit() {
    super("rdkit");
  }

  @Override
  public Analyzer createAnalyzer() {
    return new RDKitAnalyzer();
  }

  @Override
  public String description() {
    //todo: me
    return "RDKit fulltext analyzer for chemical structures.";
  }
}
