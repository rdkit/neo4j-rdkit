package org.rdkit.lucene;

import org.apache.lucene.store.Directory;

public interface DirectoryProvider {
  Directory getDirectory();
}
