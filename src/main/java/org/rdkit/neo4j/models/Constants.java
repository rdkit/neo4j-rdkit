package org.rdkit.neo4j.models;

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

import java.util.Arrays;

/**
 * Enum with necessary constants in the project
 * Node, which can be utilized for most of the procedures must obtain two labels: Chemical & Structure
 */
public enum Constants {

  Chemical("Chemical"),
  Structure("Structure"),
  IndexName("fp_index"); // the name is `fp_index` in order to support compatability with `similarity` search on `fp` property

  private final String value;

  Constants(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static Constants from(String val) {
    return Arrays.stream(Constants.values()).filter(nf -> nf.value.equals(val)).findFirst().orElseThrow(IllegalArgumentException::new);
  }



}
