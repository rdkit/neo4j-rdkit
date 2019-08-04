package org.neo4j.kernel.api.impl.fulltext.analyzer.providers;

import org.apache.lucene.analysis.Analyzer;
import org.neo4j.graphdb.index.fulltext.AnalyzerProvider;
import org.neo4j.helpers.Service;
import org.rdkit.lucene.DefaultAnalyzerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service.Implementation( AnalyzerProvider.class )
public class RDKit extends AnalyzerProvider {
  private static final Logger logger = LoggerFactory.getLogger(DefaultAnalyzerFactory.class);

  public RDKit() {
    super("rdkit");
  }

  @Override
  public Analyzer createAnalyzer() {
    return new DefaultAnalyzerFactory().createAnalyzer();
  }

  @Override
  public String description() {
    //todo: me
    return "RDKit fulltext analyzer for chemical structures.";
  }
}
