package org.rdkit.neo4j.index.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.rdkit.neo4j.index.model.ChemblRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChemicalStructureParser {
  private static final Logger logger = LoggerFactory.getLogger(ChemicalStructureParser.class);

  public static List<String> readTestData() {
    final List<String> lines = new LinkedList<>();
    final URL url = ChemicalStructureParser.class.getClassLoader().getResource("chembl_test_data.txt");

    try (InputStream is = url.openStream()) {
      BufferedReader reader = new BufferedReader(new InputStreamReader(is));
      String line;
      while ((line = reader.readLine()) != null) {
        lines.add(line);
      }

      return lines;
    } catch (IOException e) {
      logger.error("IOException during file read");
      return null;
    }
  }

  public static Map<String, Object> mapChemicalRow(String row) {
    String[] elements = row.split(" ");
    String docId = elements[0];
    String molId = elements[1];
    String smiles = elements[2];

    return new HashMap<String, Object>() {{
      put("doc_id", docId);
      put("mol_id", molId);
      put("smiles", smiles);
    }};
  }

  public static ChemblRow convertChemicalRow(String row) {
    return new ChemblRow(row.split(" "));
  }
}
