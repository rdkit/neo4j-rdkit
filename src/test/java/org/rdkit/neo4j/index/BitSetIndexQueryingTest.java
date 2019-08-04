package org.rdkit.neo4j.index;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.test.TestGraphDatabaseFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.rdkit.neo4j.index.utils.BaseTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    graphDb.execute("CALL db.index.fulltext.createNodeIndex('bitset', ['Molecule'], ['bits'], {analyzer: 'whitespace'} )");

    // build parameter maps
    List<Map<String, Object>> maps = moleculeNameToBitSetMap.entrySet().stream().map(entry -> MapUtil.map(
        "name", entry.getKey(),
        "bitsetString", entry.getValue(),
        "bits", bitsetStringToPositions(entry.getValue(), " "))).collect(Collectors.toList());

    // create test dataset
    graphDb.execute("UNWIND $maps AS map CREATE (m:Molecule) SET m=map", MapUtil.map("maps", maps));

    Result result = graphDb.execute("MATCH (n) RETURN n");
    logger.info(result.resultAsString());

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
    result = graphDb.execute("CALL db.index.fulltext.queryNodes('bitset', $query) YIELD node " +
            "WITH node, split(node.bitsetString, ' ') as bitset, split($queryBitsetString, ' ') as queryBitset " +
            "WITH node, reduce(delta=0, x in range(0,size(bitset)-1) | case bitset[x]=queryBitset[x] when true then delta else delta+1 end) as distance " +
            "RETURN node.name, node.bitsetString, node.bits, distance",
        MapUtil.map("query", queryString, "queryBitsetString", queryBitsetString));
    logger.info(result.resultAsString());

    graphDb.execute("CALL db.index.fulltext.drop('bitset')"); // otherwise we get an exception on shutdown

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
