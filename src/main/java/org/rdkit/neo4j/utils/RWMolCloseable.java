package org.rdkit.neo4j.utils;

import org.RDKit.RWMol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RWMolCloseable extends RWMol implements AutoCloseable {
  private static final Logger logger = LoggerFactory.getLogger(RWMolCloseable.class);

  public static RWMolCloseable from(final RWMol rwmol) {
    if (rwmol == null) {
      throw new IllegalArgumentException("Unable to obtain RWMol");
    }

    return new RWMolCloseable(rwmol);
  }

  private RWMolCloseable(RWMol rwmol) {
    super(rwmol);
    logger.debug("RWMolCloseable={} created from: {}", this, rwmol);
  }

  @Override
  public void close() {
    this.delete();
    logger.debug("RWMolCloseable {} closed ", this);
  }
}
