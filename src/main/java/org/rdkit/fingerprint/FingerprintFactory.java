/*
 * Copyright (C)2014, Novartis Institutes for BioMedical Research Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * - Neither the name of Novartis Institutes for BioMedical Research Inc.
 *   nor the names of its contributors may be used to endorse or promote
 *   products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.rdkit.fingerprint;

import java.util.BitSet;
import org.RDKit.RWMol;

/**
 * A fingerprint factory is an object that knows how to produce fingerprints for SMILES.
 * It is used to calculate fingerprints for the search index as well as for query structures
 * when the index is searched. As some fingerprints, e.g. Avalon, support different
 * optimizations we have two different methods for the two different purposes.
 *
 * @author Manuel Schwarze
 */
public interface FingerprintFactory {

  /**
   * Creates a query fingerprint based on the passed in SMILES.
   *
   * @param strSmiles SMILES structure, preferably canonicalized by RDKit before. Must not be null.
   *
   * @return Fingerprint as BitSet.
   */
  public BitSet createStructureFingerprint(final String strSmiles);

  /**
   * Creates a structure fingerprint based on the passed in SMILES.
   *
   * @param strSmiles SMILES structure, preferably canonicalized by RDKit before. Must not be null.
   *
   * @return Fingerprint as BitSet.
   */
  public BitSet createQueryFingerprint(final String strSmiles);

  /**
   * Method for already opened RWMol to build fingerprint from Structure settings.
   *
   * @param mol already opened RWMol object
   * @return Fingerprint as BitSet.
   */
  public BitSet createStructureFingerprint(final RWMol mol);

  /**
   * Method for already opened RWMol to build fingerprint from Query settings.
   *
   * @param mol already opened RWMol object
   * @return Fingerprint as BitSet.
   */
  public BitSet createQueryFingerprint(final RWMol mol);
}
