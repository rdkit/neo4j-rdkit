package org.rdkit.lucene;

import java.io.IOException;
import lombok.Data;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;

@Data
public class IndexOperations {

  private IndexWriterConfigFactory iwcf;
  private Directory directory;

  private IndexWriter createIndexWriter() throws IOException {
    // todo: writer can be closed and can be not

    return new IndexWriter(directory, iwcf.createIndexWriterConfig());
  }

  private IndexSearcher createIndexSearcher() throws IOException {
    return new IndexSearcher(DirectoryReader.open(directory));
  }

}
