package org.rdkit.neo4j.utils;

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

import java.io.Closeable;
import org.RDKit.RWMol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RWMolCloseable extends RWMol implements Closeable {
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
