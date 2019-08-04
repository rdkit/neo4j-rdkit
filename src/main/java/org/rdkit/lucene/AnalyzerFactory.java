package org.rdkit.lucene;

import org.apache.lucene.analysis.Analyzer;

public interface AnalyzerFactory {
  Analyzer createAnalyzer();
}
