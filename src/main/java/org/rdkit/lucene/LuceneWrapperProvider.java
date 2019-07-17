package org.rdkit.lucene;

public class LuceneWrapperProvider {
  public static LuceneWrapper createDefaultLuceneWrapper()  {
    return new LuceneWrapper(new DefaultIndexWriterConfigFactory(), new TestDirectoryProvider().getDirectory());
  }

}
