package org.rdkit.lucene;

import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.util.PriorityQueue;

final class SubstructureHitQueue extends PriorityQueue<ScoreDoc> {

  private final boolean prePopulate;

  /**
   * Creates a new instance with <code>size</code> elements. If
   * <code>prePopulate</code> is set to true, the queue will pre-populate itself
   * with sentinel objects and set its {@link #size()} to <code>size</code>. In
   * that case, you should not rely on {@link #size()} to get the number of
   * actual elements that were added to the queue, but keep track yourself.<br>
   * <b>NOTE:</b> in case <code>prePopulate</code> is true, you should pop
   * elements from the queue using the following code example:
   *
   * <pre>
   * PriorityQueue pq = new HitQueue(10, true); // pre-populate.
   * ScoreDoc top = pq.top();
   *
   * // Add/Update one element.
   * top.score = 1.0f;
   * top.doc = 0;
   * top = (ScoreDoc) pq.updateTop();
   * int totalHits = 1;
   *
   * // Now pop only the elements that were *truly* inserted.
   * // First, pop all the sentinel elements (there are pq.size() - totalHits).
   * for (int i = pq.size() - totalHits; i &gt; 0; i--) pq.pop();
   *
   * // Now pop the truly added elements.
   * ScoreDoc[] results = new ScoreDoc[totalHits];
   * for (int i = totalHits - 1; i &gt;= 0; i--) {
   *   results[i] = (ScoreDoc) pq.pop();
   * }
   * </pre>
   *
   * <p><b>NOTE</b>: This class pre-allocate a full array of
   * length <code>size</code>.
   *
   * @param size
   *          the requested size of this queue.
   * @param prePopulate
   *          specifies whether to pre-populate the queue with sentinel values.
   * @see #getSentinelObject()
   */
  SubstructureHitQueue(final int size, final boolean prePopulate) {
    super(size, prePopulate);
    this.prePopulate = prePopulate;
  }

  // Returns null if prePopulate is false.
  @Override
  protected ScoreDoc getSentinelObject() {
    // Always set the doc Id to MAX_VALUE so that it won't be favored by
    // lessThan. This generally should not happen since if score is not NEG_INF,
    // TopScoreDocCollector will always add the object to the queue.
    return !prePopulate ? null : new ScoreDoc(Integer.MAX_VALUE, Float.NEGATIVE_INFINITY);
  }

  @Override
  protected final boolean lessThan(final ScoreDoc hitA, final ScoreDoc hitB) {
    if (hitA.score == hitB.score)
      return hitA.doc > hitB.doc;
    else
      return hitA.score < hitB.score;
  }
}
