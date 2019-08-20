package org.rdkit.neo4j.models;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Constants {

  Chemical("Chemical"),
  Structure("Structure"),
  IndexName("fp_index");

  public static Constants from(String val) {
    return Arrays.stream(Constants.values()).filter(nf -> nf.value.equals(val)).findFirst().orElseThrow(IllegalArgumentException::new);
  }

  @Getter
  private final String value;
}
