package org.rdkit.lucene;

import java.io.File;
import java.io.IOException;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class TestDirectoryProvider implements DirectoryProvider {

  // todo: remove this class
  @Override
  public Directory getDirectory() {
    try {
      return FSDirectory.open(new File("test").toPath());
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }
}
