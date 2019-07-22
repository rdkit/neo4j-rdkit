package org.rdkit.lucene;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;

@AllArgsConstructor
public class LuceneWrapper {

//  private IndexWriterConfigFactory iwcf;
//  private Directory directory;
  private DocumentProvider documentProvider;
  private IndexWriter writer;
  private IndexSearcher searcher;

//  private IndexWriter createIndexWriter() throws IOException {
//    // todo: writer can be closed and can be not, think about it
//
//    return new IndexWriter(directory, iwcf.createIndexWriterConfig());
//  }
//
//  private IndexSearcher createIndexSearcher() throws IOException {
//    return new IndexSearcher(DirectoryReader.open(directory));
//  }

  public void addMolecule(final String smiles) throws IOException {

//    try (IndexWriter writer = createIndexWriter()) {
      Document doc = documentProvider.createDocument(smiles);
      writer.addDocument(doc);
//    }
  }

  public void addMolecules(final List<String> smiles) throws IOException {
//    try (IndexWriter writer = createIndexWriter()) {
      List<Document> documents = smiles.stream()
          .map(documentProvider::createDocument)
          .collect(Collectors.toList());
      writer.addDocuments(documents);
//    }
  }

  private void deleteOldValue() {
    // todo: delete old value
  }

}
