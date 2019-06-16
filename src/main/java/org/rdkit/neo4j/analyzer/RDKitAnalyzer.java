package org.rdkit.neo4j.analyzer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;

public class RDKitAnalyzer extends Analyzer {

  @Override
  protected TokenStreamComponents createComponents(String s) {
//    Tokenizer source = new FooTokenizer(reader);
//    TokenStream filter = new FooFilter(source);
//    filter = new BarFilter(filter);
//    return new TokenStreamComponents(source, filter);
    return null;
  }
}
