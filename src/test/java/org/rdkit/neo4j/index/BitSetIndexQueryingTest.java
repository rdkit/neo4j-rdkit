package org.rdkit.neo4j.index;

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

import org.junit.Test;
import org.neo4j.internal.helpers.collection.MapUtil;
import org.rdkit.neo4j.index.utils.BaseTest;
import org.rdkit.neo4j.models.LuceneQuery;
import org.rdkit.neo4j.utils.Converter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class BitSetIndexQueryingTest extends BaseTest {

  /*
      0 0 1 1 0 1 <- doc
      0 1 1 1 0 1 <- doc 2
      0 1 0 1 0 1 <- doc 3
   */
  private Map<String, String> moleculeNameToBitSetMap = MapUtil.stringMap(
      "molecule1", "0 0 1 1 0 1",
      "molecule", "0 1 1 1 0 1",
      "molecule3", "0 1 0 1 0 1"
  );

  @Test
  public void testIndexing() {

    graphDb.executeTransactionally("CREATE FULLTEXT INDEX bitset FOR (n:Molecule) ON EACH [n.bits] OPTIONS {indexConfig: {`fulltext.analyzer`: 'whitespace' } }");

    // build parameter maps
    List<Map<String, Object>> maps = moleculeNameToBitSetMap.entrySet().stream().map(entry -> MapUtil.map(
        "name", entry.getKey(),
        "bitsetString", entry.getValue(),
        "bits", bitsetStringToPositions(entry.getValue(), " "))).collect(Collectors.toList());

    // create test dataset
    graphDb.executeTransactionally("UNWIND $maps AS map CREATE (m:Molecule) SET m=map", MapUtil.map("maps", maps));

    graphDb.executeTransactionally("MATCH (n) RETURN n", Collections.emptyMap(), r -> {
      logger.info(r.resultAsString());
      return null;
    });

    // searching for molecule with "0 0 0 1 0 1"

    // build lucene query string by joining postions with AND
    String queryBitsetString = "0 0 0 1 0 1";
    String queryString = bitsetStringToPositions(queryBitsetString, " AND ");
    logger.info("querying: " + queryString);

    // querying: find all molecules with at least matching "1" on desired position
    // *AND* calculate the "distance" for each search result:
    // distance == 0: identical
    // distance == 1: the bitset differs in 1 position
    // distance == 2: the bitset differs in 2 position
    // ...

    graphDb.executeTransactionally("CALL db.index.fulltext.queryNodes('bitset', $query) YIELD node " +
                    "WITH node, split(node.bitsetString, ' ') as bitset, split($queryBitsetString, ' ') as queryBitset " +
                    "WITH node, reduce(delta=0, x in range(0,size(bitset)-1) | case bitset[x]=queryBitset[x] when true then delta else delta+1 end) as distance " +
                    "RETURN node.name, node.bitsetString, node.bits, distance",
            MapUtil.map("query", queryString, "queryBitsetString", queryBitsetString), r -> {
      logger.info(r.resultAsString());
      return null;
    });

    graphDb.executeTransactionally("DROP INDEX bitset"); // otherwise we get an exception on shutdown
  }

  @Test
  public void makeSimilarityRequestTest() throws Exception {
    insertChemblRows();

    graphDb.executeTransactionally("CREATE FULLTEXT INDEX bitset FOR (n:Chemical|Structure) ON EACH [n.fp] OPTIONS {indexConfig: {`fulltext.analyzer`: 'whitespace' } }");

    final String smiles1 = "COc1ccc(C(=O)NO)cc1";

    final Converter converter = Converter.createDefault();
    final LuceneQuery query = converter.getLuceneSimilarityQuery(smiles1, true);
    final String queryString = query.getLuceneQuery();
    final Set<String> smiles1BitPositions = new HashSet<>(Arrays.asList(queryString.split(query.getDelimiter())));

    graphDb.executeTransactionally("CALL db.index.fulltext.queryNodes('bitset', $query) "
                    + "YIELD node "
                    + "RETURN node.smiles, node.fp, node.fp_ones",
            MapUtil.map("query", queryString), result -> {
              result.stream().map(candidate -> {
                long counter = 0;
                for (String position : ((String) candidate.get("node.fp")).split("\\s")) {
                  if (smiles1BitPositions.contains(position)) counter++;
                }

                long queryPositiveBits = query.getPositiveBits();
                long candidatePositiveBits = (Long) candidate.get("node.fp_ones");
                double similarity = 1.0d * counter / (queryPositiveBits + candidatePositiveBits - counter);

                return MapUtil.map("similarity", similarity, "smiles", candidate.get("node.smiles"));
              })
              .sorted((m1, m2) -> Double.compare((Double) m2.get("similarity"), (Double) m1.get("similarity")))
              .forEach(map -> logger.info("Map={}", map));
              return null;
            });

    graphDb.executeTransactionally("CALL db.index.fulltext.queryNodes('bitset', $query) YIELD node "
                    + "RETURN node.smiles, node.fp, node.fp_ones",
            MapUtil.map("query", queryString), result -> {
              result.stream().map(candidate -> {
                long counter = 0;
                for (String position : ((String) candidate.get("node.fp")).split("\\s")) {
                  if (smiles1BitPositions.contains(position)) counter++;
                }

                long queryPositiveBits = query.getPositiveBits();
                long candidatePositiveBits = (Long) candidate.get("node.fp_ones");
                double similarity = 1.0d * counter / (queryPositiveBits + candidatePositiveBits - counter);

                return MapUtil.map("similarity", similarity, "smiles", candidate.get("node.smiles"));
              })
                      .sorted((m1, m2) -> Double.compare((Double) m2.get("similarity"), (Double) m1.get("similarity")))
                      .forEach(map -> logger.info("Map={}", map));
              return null;
            });

    graphDb.executeTransactionally("DROP INDEX bitset"); // otherwise we get an exception on shutdown
  }

  /**
   * map a string containing 0 and 1 to list of positions where 1 appears example "0 0 1 1 0 1" -> "3 4 6"
   */
  private String bitsetStringToPositions(String value, String delimiter) {
    String[] parts = value.split("\\s"); // split at whitespace
    List<String> mapped = new ArrayList<>();
    for (int i = 0; i < parts.length; i++) {
      if ("1".equals(parts[i])) {
        mapped.add(Integer.toString(i));
      }
    }
    return mapped.stream().collect(Collectors.joining(delimiter));
  }
}
