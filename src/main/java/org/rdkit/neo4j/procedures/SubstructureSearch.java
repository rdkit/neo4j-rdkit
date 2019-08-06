package org.rdkit.neo4j.procedures;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import lombok.val;
import org.RDKit.RWMol;
import org.neo4j.graphdb.*;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.procedure.*;

import org.rdkit.neo4j.models.Constants;
import org.rdkit.neo4j.models.LuceneQuery;
import org.rdkit.neo4j.models.NodeFields;
import org.rdkit.neo4j.utils.Converter;
import org.rdkit.neo4j.utils.RWMolCloseable;

public class SubstructureSearch extends BaseProcedure {
  private static final String fingerprintProperty = NodeFields.FingerprintEncoded.getValue();
  private static final String fingerprintOnesProperty = NodeFields.FingerprintOnes.getValue();
  private static final String canonicalSmilesProperty = NodeFields.CanonicalSmiles.getValue();
  private static final String indexName = Constants.IndexName.getValue();
  private static final Converter converter = Converter.createDefault();

  @Procedure(name = "org.rdkit.search.createIndex", mode = Mode.SCHEMA)
  @Description("RDKit create a nodeIndex for specific field on top of fingerprint property")
  public void createIndex(@Name("label") List<String> labelNames) {
    log.info("Create whitespace node index on `fp` property");

    db.execute(String.format("CREATE INDEX ON :%s(%s)", Constants.Chemical.getValue(), canonicalSmilesProperty));
    createFullTextIndex(indexName, labelNames, Collections.singletonList(fingerprintProperty));
  }

  @Procedure(name = "org.rdkit.search.dropIndex", mode = Mode.SCHEMA)
  @Description("Delete RDKit indexes")
  public void deleteIndex() {
    log.info("Create whitespace node index on `fp` property");

    db.execute(String.format("DROP INDEX ON :%s(%s)", Constants.Chemical.getValue(), canonicalSmilesProperty));
    db.execute("CALL db.index.fulltext.drop($index)", MapUtil.map("index", indexName));
  }

  @Procedure(name = "org.rdkit.search.substructure.smiles", mode = Mode.READ)
  @Description("RDKit substructure search based on `smiles` value")
  public Stream<NodeSSSResult> substructureSearchSmiles(@Name("label") List<String> labelNames, @Name("smiles") String smiles) {
    log.info("Substructure search smiles started :: label=%s, smiles=%s", labelNames, smiles);
    // todo: validate smiles is correct (possible)
    checkIndexExistence(labelNames, Constants.IndexName.getValue()); // if index exists, then the values are

    val query = RWMol.MolFromSmiles(smiles); // todo: memory problems here
    return findSSCandidates(query);
  }

  @Procedure(name = "org.rdkit.search.substructure.mol", mode = Mode.READ)
  @Description("RDKit substructure search based on `mol` value")
  public Stream<NodeSSSResult> substructureSearchMol(@Name("label") List<String> labelNames, @Name("mol") String mol) {
    log.info("Substructure search smiles started :: label=%s, mdlmol=%s", labelNames, mol);
    checkIndexExistence(labelNames, Constants.IndexName.getValue()); // if index exists, then the values are

    val query = RWMol.MolFromMolBlock(mol); // todo: memory problems here
    return findSSCandidates(query);
  }

  /**
   * Class wraps result of substructure search
   */
  public static class NodeSSSResult {
    public String name;
    public String luri;
    public String canonical_smiles;
    public Long score;

    public NodeSSSResult(final Map<String, Object> map, final long queryPositiveBits) {
      this.name = (String) map.getOrDefault("name", null);
      this.luri = (String) map.getOrDefault("luri", null);
      this.canonical_smiles = (String) map.get(canonicalSmilesProperty);
      long nodeCount = (Long) map.get(fingerprintOnesProperty);
      this.score = nodeCount - queryPositiveBits;
    }
  }

  /**
   * Method queries fulltext index, returns fingerprint matches and filters by substruct match
   * @param query RWMol
   * @return stream of chemical structures with substruct match
   */
  private Stream<NodeSSSResult> findSSCandidates(RWMol query) {
    query.updatePropertyCache();
    final LuceneQuery luceneQuery = converter.getLuceneSSSQuery(query);

    // added mdlmol as a returned item as sometimes it fails (probably reduces speed)
    Result result = db.execute("CALL db.index.fulltext.queryNodes($index, $query) "
            + "YIELD node "
            + "RETURN node.canonical_smiles as canonical_smiles, node.fp_ones as fp_ones, node.preferred_name as name, node.luri as luri, node.mdlmol as mdlmol",
        MapUtil.map("index", indexName, "query", luceneQuery.getLuceneQuery()));
    return result.stream()
        .filter(map -> {
          final String smiles = (String) map.get("canonical_smiles");
          try (RWMolCloseable candidate = RWMolCloseable.from(RWMol.MolFromSmiles(smiles, 0, false))) {
            candidate.updatePropertyCache(false);
            return candidate.hasSubstructMatch(query);
          } catch (Exception e) {
            log.error("Failed to convert object with smiles=%s, convert using mdmol", smiles);
            final String mdlmol = (String) map.get("mdlmol");
            try (RWMolCloseable molCandidate = RWMolCloseable.from(RWMol.MolFromMolBlock(mdlmol))) { // todo: is there any speed improvements?
              molCandidate.updatePropertyCache(false);
              return molCandidate.hasSubstructMatch(query);
            }
          }
        })
        .map(map -> new NodeSSSResult(map, luceneQuery.getPositiveBits()))
        .sorted(Comparator.comparingLong(n -> n.score));
  }

}
