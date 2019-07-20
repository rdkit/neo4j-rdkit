package org.rdkit.lucene;

import java.io.IOException;
import lombok.AllArgsConstructor;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocsCollector;
import org.rdkit.fingerprint.FingerprintFactory;

@AllArgsConstructor
public abstract class DocumentProvider {

  // todo: create advanced implementation with bits and custom query/score
  protected final FingerprintFactory fingerprintFactory;
  protected final IndexSearcher searcher;

  abstract Document createDocument(final String smiles);

  // todo: add searchDocument
  abstract TopDocsCollector<ScoreDoc> searchMoleculeByFingerprint(final String smiles, final int maxHits) throws IOException;
}
