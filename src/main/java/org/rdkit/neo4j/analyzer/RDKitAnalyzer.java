package org.rdkit.neo4j.analyzer;

import java.io.IOException;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;

public class RDKitAnalyzer extends Analyzer {

  @Override
  protected TokenStreamComponents createComponents(String s) {
    Tokenizer source = new Tokenizer() {
      @Override
      public boolean incrementToken() throws IOException {
        return false;
      }
    };
    TokenStream filter = new TokenStream(source) {
      @Override
      public boolean incrementToken() throws IOException {
        return false;
      }
    };
    filter = new TokenStream(filter) {
      @Override
      public boolean incrementToken() throws IOException {
        return false;
      }
    };
    return new TokenStreamComponents(source, filter);
  }
}
