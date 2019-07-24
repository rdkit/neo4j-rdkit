package org.rdkit.neo4j.models;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Constants {

  Chemical("Chemical"),
  Structure("Structure"),
  IndexName("rdkitIndex");

  @Getter
  private final String value;
}
