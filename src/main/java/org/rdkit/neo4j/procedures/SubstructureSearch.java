package org.rdkit.neo4j.procedures;

/*-
 * #%L
 * RDKit-Neo4j
 * %%
 * Copyright (C) 2019 RDKit
 * %%
 * Copyright (C) 2019 Evgeny Sorokin
 * @@ All Rights Reserved @@
 * This file is part of the RDKit Neo4J integration.
 * The contents are covered by the terms of the BSD license
 * which is included in the file LICENSE, found at the root
 * of the neo4j-rdkit source tree.
 * #L%
 */

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import lombok.val;
import org.RDKit.ROMol;
import org.RDKit.RWMol;
import org.neo4j.graphdb.*;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.procedure.*;

import org.rdkit.neo4j.models.Constants;
import org.rdkit.neo4j.models.LuceneQuery;
import org.rdkit.neo4j.utils.Converter;
import org.rdkit.neo4j.utils.RWMolCloseable;

/**
 * Class SubstructureSearch implements org.rdkit.search.substructure.* procedures
 * Also implements utils procedure `createIndex`, `destroyIndex`
 * todo: remove destroyIndex ?
 */
public class SubstructureSearch extends BaseProcedure {
  private static final Converter converter = Converter.createDefault(); // default converter is used for SSS

  /**
   * Procedure builts property index for label {@link Constants#Chemical} on {@link #canonicalSmilesProperty} property
   * Procedure builts fulltext index for specified labels on {@link #fingerprintProperty} property.
   * @param labelNames - node labels
   */
  @Procedure(name = "org.rdkit.search.createIndex", mode = Mode.SCHEMA)
  @Description("RDKit create a nodeIndex for specific field on top of fingerprint property")
  public void createIndex(@Name("label") List<String> labelNames) {
    log.info("Create whitespace node index on `fp` property");

    db.execute(String.format("CREATE INDEX ON :%s(%s)", Constants.Chemical.getValue(), canonicalSmilesProperty));
    createFullTextIndex(indexName, labelNames, Collections.singletonList(fingerprintProperty));
  }

  /**
   * Procedure deletes fulltext index and property index created by procedure above {@link #createIndex(List)}
   */
  @Procedure(name = "org.rdkit.search.dropIndex", mode = Mode.SCHEMA)
  @Description("Delete RDKit indexes")
  public void deleteIndex() {
    log.info("Create whitespace node index on `fp` property");

    db.execute(String.format("DROP INDEX ON :%s(%s)", Constants.Chemical.getValue(), canonicalSmilesProperty));
    db.execute("CALL db.index.fulltext.drop($index)", MapUtil.map("index", indexName));
  }

  /**
   * Procedure implements SSS based on `smiles` value
   * Method converts specified smiles into fingerprint and uses its value as an input for fulltext search
   * Default converter is used {@link Converter#createDefault()}
   *
   * @param labelNames - node labels to search on top of
   * @param smiles - value to transform and use during SSS
   * @param sanitize
   * @return obtained nodes
   */
  @Procedure(name = "org.rdkit.search.substructure.smiles", mode = Mode.READ)
  @Description("RDKit substructure search based on `smiles` value")
  public Stream<NodeSSSResult> substructureSearchSmiles(@Name("label") List<String> labelNames, @Name("smiles") String smiles,
                                                        @Name(value="sanitize", defaultValue="true") boolean sanitize) {
    log.info("Substructure search smiles started :: label=%s, smiles=%s", labelNames, smiles);
    checkIndexExistence(labelNames, Constants.IndexName.getValue()); // if index exists, then the values are

    RWMol query;
    try {
      query = RWMol.MolFromSmiles(smiles,0, sanitize); // todo: it is unknown when the query object is freed
      if (query == null)
        throw new IllegalArgumentException("Unable to convert specified smiles");
    } catch (Exception e) {
      throw new IllegalArgumentException("Unable to convert specified smiles");
    }
    return findSSCandidates(query, sanitize);
  }

  /**
   * Procedure implements SSS based on `mol` value
   * Method converts specified mol value into fingerprint and uses its value as an input for fulltext search
   * Default converter is used {@link Converter#createDefault()}
   *
   * @param labelNames - node labels
   * @param mol - mdlmol block value
   * @return obtained nodes
   */
  @Procedure(name = "org.rdkit.search.substructure.mol", mode = Mode.READ)
  @Description("RDKit substructure search based on `mol` value")
  public Stream<NodeSSSResult> substructureSearchMol(@Name("label") List<String> labelNames, @Name("mol") String mol,
                                                     @Name(value="sanitize", defaultValue="true") boolean sanitize) {
    log.info("Substructure search smiles started :: label=%s, mdlmol=%s", labelNames, mol);
    checkIndexExistence(labelNames, Constants.IndexName.getValue()); // if index exists, then the values are

    ROMol query = createBlockedROMolFromMol(mol);
    return findSSCandidates(query, sanitize);
  }


  /**
   * User function which return boolean value - is there a substructure match between two chemical structures
   *
   * @param candidate - node object with {@link org.rdkit.neo4j.models.NodeFields} parameters
   * @param smiles - to be converted into chemical structure and compared with
   * @return existence substructure match
   */
  @UserFunction(name = "org.rdkit.search.substructure.is.smiles")
  @Description("RDKit function checks substructure match between two chemical structures (provided node and specified smiles)")
  public boolean isSubstructure(@Name("candidate") Node candidate, @Name("substructure_smiles") String smiles,
                                @Name(value="sanitize", defaultValue="true") boolean sanitize) {
    final String luri = (String) candidate.getProperty("luri", "<undefined>");
    log.info("isSubstructure call based on candidate_luri=%s, substructure_smiles=%s", luri, smiles);

    try (val query = RWMolCloseable.from(RWMol.MolFromSmiles(smiles, 0, sanitize))) {
      query.updatePropertyCache(false);
      final String candidateSmiles = (String) candidate.getProperty("canonical_smiles");
      try (val candidateRWMol = RWMolCloseable.from(RWMol.MolFromSmiles(candidateSmiles, 0, sanitize))) {
        candidateRWMol.updatePropertyCache(false);
        return candidateRWMol.hasSubstructMatch(query);
      }
    }
  }

  @UserFunction(name = "org.rdkit.search.substructure.is.mol")
  @Description("RDKit function checks substructure match between two chemical structures (provided node and specified mol block)")
  public boolean isSubstructureMol(@Name("candidate") Node candidate, @Name("substructure_mol") String mol) {
    final String luri = (String) candidate.getProperty("luri", "<undefined>");
    log.info("isSubstructure call based on candidate_luri=%s, substructure_mol=%s", luri, mol);

    ROMol query = createBlockedROMolFromMol(mol);
    query.updatePropertyCache(false);

    final String candidateSmiles = (String) candidate.getProperty("canonical_smiles");
    try (val candidateRWMol = RWMolCloseable.from(RWMol.MolFromSmiles(candidateSmiles, 0, false))) {
      candidateRWMol.updatePropertyCache(false);
      return candidateRWMol.hasSubstructMatch(query);
    } finally {
      query.delete();
    }
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
   * @param sanitize
   * @return stream of chemical structures with substruct match
   */
  private Stream<NodeSSSResult> findSSCandidates(ROMol query, boolean sanitize) {
    query.updatePropertyCache();
    final LuceneQuery luceneQuery = converter.getLuceneSSSQuery(query, sanitize);

    // added mdlmol as a returned item as sometimes it fails (probably reduces speed)
    Result result = db.execute("CALL db.index.fulltext.queryNodes($index, $query) "
            + "YIELD node "
            + "RETURN node.canonical_smiles as canonical_smiles, node.fp_ones as fp_ones, node.preferred_name as name, node.luri as luri",
        MapUtil.map("index", indexName, "query", luceneQuery.getLuceneQuery()));
    return result.stream()
        .filter(map -> {
          final String smiles = (String) map.get("canonical_smiles");
          try (RWMolCloseable candidate = RWMolCloseable.from(RWMol.MolFromSmiles(smiles, 0, sanitize))) {
            candidate.updatePropertyCache(false);
            return candidate.hasSubstructMatch(query);
          } catch (Exception e) {
            log.error("Failed to convert object with smiles=%s, convert using mdmol", smiles);
            final String mdlmol = (String) db.findNode(Label.label("Chemical"), canonicalSmilesProperty, smiles).getProperty("mdlmol"); // cheaper solution, as it is very rare
            try (RWMolCloseable molCandidate = RWMolCloseable.from(RWMol.MolFromMolBlock(mdlmol))) { // todo: is there any speed improvements?
              molCandidate.updatePropertyCache(false);
              return molCandidate.hasSubstructMatch(query);
            }
          }
        })
//        .parallel()
        .map(map -> new NodeSSSResult(map, luceneQuery.getPositiveBits()))
        .sorted(Comparator.comparingLong(n -> n.score));
  }

  /**
   * Method creates `blocking` rwmol from mol block
   * todo: currently MANUAL memory free!
   * @param mol to create ROMol from
   * @return ROMol with blocked H's
   */
  private ROMol createBlockedROMolFromMol(final String mol) {
    ROMol query;
    try {
      query = RWMol.MolFromMolBlock(mol, true,false); // todo: it is unknown when the query object is freed
      if (query == null)
        throw new IllegalArgumentException("Unable to convert specified mol");
      return query.mergeQueryHs();
    } catch (Exception e) {
      throw new IllegalArgumentException("Unable to convert specified mol");
    }
  }
}
