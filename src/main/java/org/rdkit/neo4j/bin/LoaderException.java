package org.rdkit.neo4j.bin;

public class LoaderException extends Exception {
  public LoaderException(String s) {
    super(s);
  }

  public LoaderException(String s, Throwable t) {
    super(s, t);
  }
}
