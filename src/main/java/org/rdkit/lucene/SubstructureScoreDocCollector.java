package org.rdkit.lucene;

import java.io.IOException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopDocsCollector;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.util.PriorityQueue;

/**
 * A {@link Collector} implementation that collects the top-scoring hits,
 * returning them as a {@link TopDocs}. This is used by {@link IndexSearcher} to
 * implement {@link TopDocs}-based search. Hits are sorted by score descending
 * and then (when the scores are tied) docID ascending. When you create an
 * instance of this collector you should know in advance whether documents are
 * going to be collected in doc Id order or not.
 *
 * <p>
 * <b>NOTE</b>: The values {@link Float#NaN} and {Float#NEGATIVE_INFINITY} are
 * not valid scores. This collector will not properly collect hits with such
 * scores.
 */
public abstract class SubstructureScoreDocCollector extends TopDocsCollector<ScoreDoc> {

  protected SubstructureScoreDocCollector(PriorityQueue<ScoreDoc> pq) {
    super(pq);
  }

  // Assumes docs are scored in order.
//  private static class InOrderTopScoreDocCollector extends SubstructureScoreDocCollector {
//    private InOrderTopScoreDocCollector(final int numHits) {
//      super(numHits);
//    }
//
//    @Override
//    public void collect(final int doc, final float score) throws IOException {
//      // This collector cannot handle these scores:
//      assert score != Float.NEGATIVE_INFINITY;
//      assert !Float.isNaN(score);
//
//      totalHits++;
//      if (score <= m_pqTop.score) {
//        // Since docs are returned in-order (i.e., increasing doc Id), a
//        // document
//        // with equal score to pqTop.score cannot compete since HitQueue
//        // favors
//        // documents with lower doc Ids. Therefore reject those docs
//        // too.
//        return;
//      }
//      m_pqTop.doc = doc + m_docBase;
//      m_pqTop.score = score;
//      m_pqTop = pq.updateTop();
//    }
//
//    @Override
//    public boolean acceptsDocsOutOfOrder() {
//      return false;
//    }
//  }
//
//  // Assumes docs are scored in order.
//  private static class InOrderPagingScoreDocCollector extends SubstructureScoreDocCollector {
//    private final ScoreDoc after;
//    // this is always after.doc - docBase, to save an add when score ==
//    // after.score
//    private int afterDoc;
//    private int collectedHits;
//
//    private InOrderPagingScoreDocCollector(final ScoreDoc after, final int numHits) {
//      super(numHits);
//      this.after = after;
//    }
//
//    @Override
//    public void collect(final int doc, final float score) throws IOException {
//      // This collector cannot handle these scores:
//      assert score != Float.NEGATIVE_INFINITY;
//      assert !Float.isNaN(score);
//
//      totalHits++;
//
//      if (score > after.score
//          || (score == after.score && doc <= afterDoc)) {
//        // hit was collected on a previous page
//        return;
//      }
//
//      if (score <= m_pqTop.score) {
//        // Since docs are returned in-order (i.e., increasing doc Id), a
//        // document
//        // with equal score to pqTop.score cannot compete since HitQueue
//        // favors
//        // documents with lower doc Ids. Therefore reject those docs
//        // too.
//        return;
//      }
//      collectedHits++;
//      m_pqTop.doc = doc + m_docBase;
//      m_pqTop.score = score;
//      m_pqTop = pq.updateTop();
//    }
//
//    @Override
//    public boolean acceptsDocsOutOfOrder() {
//      return false;
//    }
//
//    @Override
//    public void setNextReader(final IndexReader reader, final int base) {
//      super.setNextReader(reader, base);
//      afterDoc = after.doc - m_docBase;
//    }
//
//    @Override
//    protected int topDocsSize() {
//      return collectedHits < pq.size() ? collectedHits : pq.size();
//    }
//
//    @Override
//    protected TopDocs newTopDocs(final ScoreDoc[] results, final int start) {
//      return results == null ? new TopDocs(totalHits, new ScoreDoc[0],
//          Float.NaN) : new TopDocs(totalHits, results, Float.NaN);
//    }
//  }
//
//  // Assumes docs are scored out of order.
//  private static class OutOfOrderTopScoreDocCollector extends SubstructureScoreDocCollector {
//    private OutOfOrderTopScoreDocCollector(final int numHits) {
//      super(numHits);
//    }
//
//    @Override
//    public void collect(int doc, final float score) throws IOException {
//      // This collector cannot handle NaN
//      assert !Float.isNaN(score);
//
//      totalHits++;
//      if (score < m_pqTop.score) {
//        // Doesn't compete w/ bottom entry in queue
//        return;
//      }
//      doc += m_docBase;
//      if (score == m_pqTop.score && doc > m_pqTop.doc) {
//        // Break tie in score by doc ID:
//        return;
//      }
//      m_pqTop.doc = doc;
//      m_pqTop.score = score;
//      m_pqTop = pq.updateTop();
//    }
//
//    @Override
//    public boolean acceptsDocsOutOfOrder() {
//      return true;
//    }
//  }
//
//  // Assumes docs are scored out of order.
//  private static class OutOfOrderPagingScoreDocCollector extends SubstructureScoreDocCollector {
//    private final ScoreDoc after;
//    // this is always after.doc - docBase, to save an add when score ==
//    // after.score
//    private int afterDoc;
//    private int collectedHits;
//
//    private OutOfOrderPagingScoreDocCollector(final ScoreDoc after, final int numHits) {
//      super(numHits);
//      this.after = after;
//    }
//
//    @Override
//    public void collect(int doc, final float score) throws IOException {
//      // This collector cannot handle NaN
//      assert !Float.isNaN(score);
//
//      totalHits++;
//      if (score > after.score
//          || (score == after.score && doc <= afterDoc)) {
//        // hit was collected on a previous page
//        return;
//      }
//      if (score < m_pqTop.score) {
//        // Doesn't compete w/ bottom entry in queue
//        return;
//      }
//      doc += m_docBase;
//      if (score == m_pqTop.score && doc > m_pqTop.doc) {
//        // Break tie in score by doc ID:
//        return;
//      }
//      collectedHits++;
//      m_pqTop.doc = doc;
//      m_pqTop.score = score;
//      m_pqTop = pq.updateTop();
//    }
//
//    @Override
//    public boolean acceptsDocsOutOfOrder() {
//      return true;
//    }
//
//    @Override
//    public void setNextReader(final IndexReader reader, final int base) {
//      super.setNextReader(reader, base);
//      afterDoc = after.doc - m_docBase;
//    }
//
//    @Override
//    protected int topDocsSize() {
//      return collectedHits < pq.size() ? collectedHits : pq.size();
//    }
//
//    @Override
//    protected TopDocs newTopDocs(final ScoreDoc[] results, final int start) {
//      return results == null ? new TopDocs(totalHits, new ScoreDoc[0],
//          Float.NaN) : new TopDocs(totalHits, results, Float.NaN);
//    }
//  }
//
//  /**
//   * Creates a new {@link TopScoreDocCollector} given the number of hits to
//   * collect and whether documents are scored in order by the input
//   * {@link Scorer} to {@link #setScorer(Scorer)}.
//   *
//   * <p>
//   * <b>NOTE</b>: The instances returned by this method pre-allocate a full
//   * array of length <code>numHits</code>, and fill the array with sentinel
//   * objects.
//   */
//  public static SubstructureScoreDocCollector create(final int numHits,
//      final boolean docsScoredInOrder) {
//    return create(numHits, null, docsScoredInOrder);
//  }
//
//  /**
//   * Creates a new {@link TopScoreDocCollector} given the number of hits to
//   * collect, the bottom of the previous page, and whether documents are
//   * scored in order by the input {@link Scorer} to {@link #setScorer(Scorer)}
//   * .
//   *
//   * <p>
//   * <b>NOTE</b>: The instances returned by this method pre-allocate a full
//   * array of length <code>numHits</code>, and fill the array with sentinel
//   * objects.
//   */
//  public static SubstructureScoreDocCollector create(final int numHits,
//      final ScoreDoc after, final boolean docsScoredInOrder) {
//
//    if (numHits <= 0) {
//      throw new IllegalArgumentException(
//          "numHits must be > 0; please use TotalHitCountCollector if you just need the total hit count");
//    }
//
//    if (docsScoredInOrder) {
//      return after == null ? new InOrderTopScoreDocCollector(numHits)
//          : new InOrderPagingScoreDocCollector(after, numHits);
//    }
//    else {
//      return after == null ? new OutOfOrderTopScoreDocCollector(numHits)
//          : new OutOfOrderPagingScoreDocCollector(after, numHits);
//    }
//
//  }
//
//  protected ScoreDoc m_pqTop;
//  protected int m_docBase = 0;
//  protected Scorer m_scorer;
//
//  // prevents instantiation
//  private SubstructureScoreDocCollector(final int numHits) {
//    super(new SubstructureHitQueue(numHits, true));
//    // HitQueue implements getSentinelObject to return a ScoreDoc, so we
//    // know that at this point top() is already initialized.
//    m_pqTop = pq.top();
//  }
//
//  @Override
//  protected TopDocs newTopDocs(final ScoreDoc[] results, final int start) {
//    if (results == null) {
//      return EMPTY_TOPDOCS;
//    }
//
//    // We need to compute maxScore in order to set it in TopDocs. If start
//    // == 0,
//    // it means the largest element is already in results, use its score as
//    // maxScore. Otherwise pop everything else, until the largest element is
//    // extracted and use its score as maxScore.
//    float maxScore = Float.NaN;
//    if (start == 0) {
//      maxScore = results[0].score;
//    }
//    else {
//      for (int i = pq.size(); i > 1; i--) {
//        pq.pop();
//      }
//      maxScore = pq.pop().score;
//    }
//
//    return new TopDocs(totalHits, results, maxScore);
//  }
//
//  @Override
//  public void collect(final int doc) throws IOException {
//    collect(doc, m_scorer.score());
//  }
//
//  /**
//   * Collect method, which allows passing in a score.
//   *
//   * @param doc Document ID to collect.
//   * @param score Score to be used.
//   *
//   * @throws IOException Thrown, if index access failed.
//   */
//  public abstract void collect(int doc, float score) throws IOException;
//
//  @Override
//  public void setNextReader(final IndexReader reader, final int base) {
//    m_docBase = base;
//  }
//
//  @Override
//  public void setScorer(final Scorer scorer) throws IOException {
//    this.m_scorer = scorer;
//  }
}
