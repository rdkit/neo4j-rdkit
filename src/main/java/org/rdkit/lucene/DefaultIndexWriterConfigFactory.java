package org.rdkit.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexWriterConfig;

public class DefaultIndexWriterConfigFactory implements IndexWriterConfigFactory {

  private AnalyzerFactory factory;

  public DefaultIndexWriterConfigFactory() {
    this.factory = new DefaultAnalyzerFactory();
  }

  @Override
  public IndexWriterConfig createIndexWriterConfig() {
    return new IndexWriterConfig(factory.createAnalyzer());
  }
}
