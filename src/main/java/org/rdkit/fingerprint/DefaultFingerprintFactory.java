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

import java.util.BitSet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.val;
import org.RDKit.ExplicitBitVect;
import org.RDKit.ROMol;
import org.RDKit.RWMol;
import org.rdkit.neo4j.utils.RWMolCloseable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A fingerprint factory is an object that knows how to produce fingerprints for SMILES. It is used to calculate fingerprints for the search index as
 * well as for query structures when the index is searched. As some fingerprints, e.g. Avalon, support different optimizations we have two different
 * methods for the two different purposes.
 *
 * @author Manuel Schwarze
 */

@Data
@AllArgsConstructor
public class DefaultFingerprintFactory implements FingerprintFactory {

  //
  // Constants
  //

  /**
   * The logger instance.
   */
  private static final Logger logger = LoggerFactory.getLogger(DefaultFingerprintFactory.class);

  //
  // Members
  //

  /**
   * The settings to be used for calculating structure fingerprints with this factory.
   */
  private final FingerprintSettings settingsStructure;

  /**
   * The settings to be used for calculating query fingerprints with this factory.
   */
  private final FingerprintSettings settingsQuery;

  //
  // Constructors
  //

  /**
   * Creates a new fingerprint factory based on the past in settings. Structure and query fingerprints are handled the same way. There is distinction
   * between them. To handle them differently, use the other constructor.
   *
   * @param settings Fingerprint settings. Must not be null.
   */
  public DefaultFingerprintFactory(@NonNull final FingerprintSettings settings) {
    settingsStructure = settingsQuery = settings;
  }

  //
  // Public Methods
  //

  /**
   * Creates a fingerprint based on the passed in SMILES.
   *
   * @param strSmiles SMILES structure, preferably canonicalized by RDKit before. Must not be null.
   * @param sanitize
   * @return Fingerprint as BitSet.
   */
  @Override
  public BitSet createStructureFingerprint(final String strSmiles, boolean sanitize) {
    return createFingerprint(strSmiles, settingsStructure, sanitize);
  }

  /**
   * Creates a fingerprint based on the passed in SMILES.
   *
   * @param strSmiles SMILES structure, preferably canonicalized by RDKit before. Must not be null.
   * @param sanitize
   * @return Fingerprint as BitSet.
   */
  @Override
  public BitSet createQueryFingerprint(final String strSmiles, boolean sanitize) {
    return createFingerprint(strSmiles, settingsQuery, sanitize);
  }

  /**
   * Method for already opened RWMol to build fingerprint from Query settings.
   *
   * @param mol already opened RWMol object
   * @return Fingerprint as BitSet.
   */
  public BitSet createQueryFingerprint(final ROMol mol) {
    return createFingerprint(mol, settingsQuery);
  }

  /**
   * Method for already opened RWMol to build fingerprint from Structure settings.
   *
   * @param mol already opened RWMol object
   * @return Fingerprint as BitSet.
   */
  public BitSet createStructureFingerprint(final ROMol mol) {
    return createFingerprint(mol, settingsStructure);
  }

  //
  // Private Methods
  //

  /**
   * Creates a fingerprint based on the passed in SMILES.
   *
   * @param strSmiles SMILES structure, preferably canonicalized by RDKit before. Must not be null. ! EXPECTED CANONICALIZED SMILES !
   * @param settings Fingerprint settings to be used.
   * @param sanitize
   * @return Fingerprint as BitSet.
   */
  private BitSet createFingerprint(@NonNull final String strSmiles, final FingerprintSettings settings, boolean sanitize) {

    // todo: update code if other types are used

    // Normally: ROMol objects are needed to calculate fingerprints
    // Create an ROMol object

    // Performance trick, if SMILES is already canonicalized
    try (val mol = RWMolCloseable.from(RWMol.MolFromSmiles(strSmiles, 0, sanitize))) {
      return createFingerprint(mol, settings);
    }
  }

  /**
   * Method for already opened RWMol
   * @param mol - canonicalized
   * @param settings to build fingerprint from
   * @return BitSet from rwmol (fingerprint of `settings` type)
   */
  private BitSet createFingerprint(final ROMol mol, final FingerprintSettings settings) {
    mol.updatePropertyCache();

    // Calculate fingerprint
    return convert(settings.getRdkitFingerprintType().calculate(mol, settings));
  }

  /**
   * Converts an RDKit bit vector into a Java BitSet object.
   *
   * @param rdkitBitVector RDKit (C++ based) bit vector. Can be null.
   * @return BitSet or null, if null was passed in.
   */
  private BitSet convert(final ExplicitBitVect rdkitBitVector) {
    BitSet fingerprint = null;

    if (rdkitBitVector != null) {
      final int iLength = (int) rdkitBitVector.getNumBits();
      fingerprint = new BitSet(iLength);
      for (int i = 0; i < iLength; i++) {
        if (rdkitBitVector.getBit(i)) {
          fingerprint.set(i);
        }
      }
    }

    return fingerprint;
  }
}
