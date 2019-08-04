package org.rdkit.lucene;

import java.io.IOException;
import java.util.BitSet;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocsCollector;
import org.apache.lucene.search.TopScoreDocCollector;
import org.rdkit.fingerprint.FingerprintFactory;

public class StringDocumentProvider extends DocumentProvider {

  private final String FIELD_FP = "fingerprint";

  public StringDocumentProvider(final IndexSearcher indexSearcher, final FingerprintFactory fingerprintFactory) {
    super(fingerprintFactory, indexSearcher);
  }

  public Document createDocument(final String smiles) {
    Document document = new Document();
    BitSet fingerprint = fingerprintFactory.createStructureFingerprint(smiles);

    // todo: add here old implementation
    for (int i = fingerprint.nextSetBit(0); i >= 0; i = fingerprint.nextSetBit(i + 1)) {
      document.add(new Field(FIELD_FP, Integer.toString(i), Store.NO,
            Index.NOT_ANALYZED_NO_NORMS, Field.TermVector.NO));
    }
    return document;
  }

  public TopDocsCollector<ScoreDoc> searchMoleculeByFingerprint(final String smiles, final int maxHits) throws IOException {
    TopScoreDocCollector collector = null;

    if (searcher != null) {
      // Calculate query fingerprint
      final BitSet fpQuery = fingerprintFactory.createQueryFingerprint(smiles);

      if (fpQuery != null) {
        // Create query for checking if all query fingerprint bit positions
        // are matching set bits in a molecules fingerprint
        final BooleanQuery query = new BooleanQuery();
        for (int i = fpQuery.nextSetBit(0); i >= 0; i = fpQuery
            .nextSetBit(i + 1)) {
          query.add(new BooleanClause(new TermQuery(new Term(FIELD_FP,
              Integer.toString(i))), BooleanClause.Occur.MUST));
        }

        // Perform the search
        collector = TopScoreDocCollector.create(maxHits);
        searcher.search(query, collector);
      }
    }

    return collector;
  }

}
