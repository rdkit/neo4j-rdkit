package org.rdkit.neo4j.procedures;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Stream;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.IndexSearcher;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.internal.kernel.api.IndexReference;
import org.neo4j.internal.kernel.api.exceptions.schema.IndexNotFoundKernelException;
import org.neo4j.kernel.api.KernelTransaction;
import org.neo4j.kernel.api.impl.fulltext.FulltextAdapter;
import org.neo4j.kernel.impl.newapi.AllStoreHolder;
import org.neo4j.logging.Log;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;
import org.neo4j.storageengine.api.EntityType;
import org.neo4j.storageengine.api.schema.IndexReader;
import org.rdkit.lucene.LuceneWrapper;
import org.rdkit.neo4j.procedures.ExactSearch.NodeWrapper;
import org.rdkit.neo4j.utils.Converter;

public class SubstructureSearch {
  @Context
  public GraphDatabaseService db;

  @Context
  public Log log;

  @Context
  public KernelTransaction tx;

  @Context
  public FulltextAdapter accessor;


  @Procedure(name = "org.rdkit.search.substructure.smiles", mode = Mode.READ)
  @Description("RDKit substructure search based on `smiles` property")
  public Stream<NodeWrapper> substructureSearch(@Name("label") List<String> labelNames, @Name("smiles") String smiles) {
    log.info("Substructure search smiles :: label={}, smiles={}", labelNames, smiles);

    // todo: validate smiles is correct (possible)

    final String rdkitSmiles = Converter.getRDKitSmiles(smiles);
    final String labels = String.join(":", labelNames);

    Result result = null; // todo: IMPLEMENT
    return result.stream().map(NodeWrapper::new);
  }

  @Procedure(name = "org.rdkit.lucene.update", mode = Mode.WRITE)
  @Description("RDKit substructure search based on `smiles` property")
  public Stream<NodeWrapper> updateLuceneIndex(@Name("label") List<String> labelNames) {
    log.info("Lucene update index :: label={}", labelNames);

    LuceneFulltextIndexInfo lfii = new LuceneFulltextIndexInfo(tx, "rdkit");
    IndexSearcher searcher = lfii.getIndexSearcher();
    IndexWriter writer = null;
    LuceneWrapper wrapper = new LuceneWrapper(null, writer, searcher);
    return null; // todo: me
  }

  public static class LuceneFulltextIndexInfo {

    private final IndexReader indexReader;
    private final Field analyzerField;
    private final Method getIndexSearcherMethod;
    private final EntityType entityType;

    public LuceneFulltextIndexInfo(KernelTransaction tx, String indexName) {
      try {
        IndexReference indexReference = tx.schemaRead().indexGetForName(indexName);
        entityType = indexReference.schema().entityType();

        AllStoreHolder allStoreHolder = (AllStoreHolder) tx.dataRead();
        indexReader = allStoreHolder.indexReader(indexReference, false);

        Class<?> simpleFulltextIndexReaderClass = Class.forName("org.neo4j.kernel.api.impl.fulltext.SimpleFulltextIndexReader");
        getIndexSearcherMethod = simpleFulltextIndexReaderClass.getDeclaredMethod("getIndexSearcher");
        getIndexSearcherMethod.setAccessible(true);

        analyzerField = simpleFulltextIndexReaderClass.getDeclaredField("analyzer");
        analyzerField.setAccessible(true);

      } catch (ClassNotFoundException | NoSuchFieldException | IndexNotFoundKernelException | NoSuchMethodException e) {
        throw new RuntimeException(e);
      }
    }

    private Object invoke(Method m, Object args) {
      try {
        return m.invoke(args);
      } catch (IllegalAccessException| InvocationTargetException e) {
        throw new RuntimeException(e);
      }
    }

    public IndexSearcher getIndexSearcher() {
      return (IndexSearcher) invoke(getIndexSearcherMethod, indexReader);
    }

    public Analyzer getAnalyzer() {
      try {
        return (Analyzer) analyzerField.get(indexReader);
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }

    public EntityType getEntityType() {
      return entityType;
    }
  }
}
