package org.rdkit.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexWriterConfig;

public interface IndexWriterConfigFactory {
  IndexWriterConfig createIndexWriterConfig();
}
