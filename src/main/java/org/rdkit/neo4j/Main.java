package org.rdkit.neo4j;

import org.RDKit.RDKFuncs;
import org.rdkit.neo4j.bin.LibraryLoader;
import org.rdkit.neo4j.exceptions.LoaderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
  private static final Logger logger = LoggerFactory.getLogger(Main.class);


  public static void main(String[] args) {
    try {
      logger.info("And now we check .jar file to explode");
      LibraryLoader.loadLibraries();
      String propName = RDKFuncs.getComputedPropName();
      logger.info("Native call returned: {}", propName);
    } catch (LoaderException e) {
      logger.error("LoaderException occured: ", e);
      e.printStackTrace();
    }
  }

}
