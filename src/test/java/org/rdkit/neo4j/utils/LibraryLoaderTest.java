package org.rdkit.neo4j.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class LibraryLoaderTest {

  @Test
  public void testWin64() {
    final String expectedOs = "win32";
    final String expectedArch = "x86_64";

    String os = LibraryLoader.getOs();
    String arch = LibraryLoader.getArch();

    assertEquals(os, expectedOs);
    assertEquals(arch, expectedArch);
  }
}
