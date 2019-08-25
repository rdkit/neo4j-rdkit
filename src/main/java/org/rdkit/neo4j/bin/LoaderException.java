package org.rdkit.neo4j.bin;

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

public class LoaderException extends Exception {
  public LoaderException(String s) {
    super(s);
  }

  public LoaderException(String s, Throwable t) {
    super(s, t);
  }
}
