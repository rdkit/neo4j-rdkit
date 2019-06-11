package org.rdkit.neo4j.exceptions;

public class LoaderException extends Exception {
  public LoaderException(String s) {
    super(s);
  }

  public LoaderException(String s, Throwable t) {
    super(s, t);
  }
}
