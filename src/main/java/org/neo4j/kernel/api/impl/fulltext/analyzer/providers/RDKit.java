package org.neo4j.kernel.api.impl.fulltext.analyzer.providers;

import org.apache.lucene.analysis.Analyzer;
import org.neo4j.graphdb.index.fulltext.AnalyzerProvider;
import org.neo4j.helpers.Service;
import org.rdkit.neo4j.analyzer.RDKitAnalyzer;
import org.rdkit.neo4j.bin.LibraryLoader;
import org.rdkit.neo4j.exceptions.LoaderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service.Implementation( AnalyzerProvider.class )
public class RDKit extends AnalyzerProvider {
  private static final Logger logger = LoggerFactory.getLogger(RDKitAnalyzer.class);

  static {
    try {
      LibraryLoader.loadLibraries();
    } catch (LoaderException e) {
      logger.error("Unable to load native libraries");
    }
  }

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
