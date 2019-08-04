package org.rdkit.lucene;

import java.io.IOException;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

public class DefaultAnalyzerFactory implements AnalyzerFactory {

  @Override
  public Analyzer createAnalyzer() {
    return new StandardAnalyzer();
  }
}
