package org.rdkit.lucene;

import java.io.IOException;
import java.util.BitSet;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.RDKit.ExplicitBitVect;
import org.RDKit.RDKFuncs;
import org.RDKit.RWMol;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryparser.xml.builders.BooleanQueryBuilder;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NGramPhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.BytesRef;
import org.rdkit.neo4j.utils.RWMolCloseable;

@AllArgsConstructor
public class LuceneWrapper {

  private IndexWriterConfigFactory iwcf;
  private Directory directory;

  private IndexWriter createIndexWriter() throws IOException {
    // todo: writer can be closed and can be not

    return new IndexWriter(directory, iwcf.createIndexWriterConfig());
  }

  private IndexSearcher createIndexSearcher() throws IOException {
    return new IndexSearcher(DirectoryReader.open(directory));
  }

  public void addMolecule(final String smiles) {

    try (IndexWriter writer = createIndexWriter()) {
      Document doc = createDocument(smiles);
      writer.addDocument(doc);
    }

  }

  public void addMolecules(final List<String> smiles) {
    try (IndexWriter writer = createIndexWriter()) {
      List<Document> documents = smiles.stream()
          .map(this::createDocument)
          .collect(Collectors.toList());
      writer.addDocuments(documents);
    }
  }

  private void deleteOldValue() {
    // todo: delete old value
  }

  private Document createDocument(final String smiles) {
    try (RWMolCloseable rwmol = RWMolCloseable.from(RWMol.MolFromSmiles(smiles, 0, false))) {
      Document document = new Document();
      // todo: what about other fingeprint types?
      // todo: add fingerprint settings
      // todo: add fingerprint factory?
      final ExplicitBitVect bitVector = new ExplicitBitVect(length);
      RDKFuncs.getAvalonFP(rwmol, bitVector, length, true, true, avalonbitflags);

      BitSet fingerprint = convert(bitVector);

      // todo: what kind of field to use?
      document.add(new Field);
      return document;
    }
  }

  private void none() {
    Query q;
    BooleanQuery.Builder builder = new BooleanQuery.Builder();
    Query
  }

  /**
   * Converts an RDKit bit vector into a Java BitSet object.
   *
   * @param rdkitBitVector RDKit (C++ based) bit vector. Can be null.
   *
   * @return BitSet or null, if null was passed in.
   */
  private BitSet convert(final ExplicitBitVect rdkitBitVector) {
    BitSet fingerprint = null;

    if (rdkitBitVector != null) {
      final int iLength = (int)rdkitBitVector.getNumBits();
      fingerprint = new BitSet(iLength);
      for (int i = 0; i < iLength; i++) {
        if (rdkitBitVector.getBit(i)) {
          fingerprint.set(i);
        }
      }
    }

    return fingerprint;
  }
}
