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


/**
 * Defines fingerprint settings used to calculate fingerprints. Not all settings
 * are used for all types of fingerprints. If a string setting is unavailable
 * null should be returned. For numeric settings this is not possible, therefore
 * the constant {@link #UNAVAILABLE} has been introduced, which shall be used
 * to express that a setting is not defined. The value of {@link #UNAVAILABLE}
 * may change over time, if the current value is needed in the future for a new
 * type of settings.
 * 
 * @author Manuel Schwarze
 */
public interface FingerprintSettings {

	/** The integer value to be used if a property is not set. (like null) */
	public static final int UNAVAILABLE = -1;

	/**
	 * Returns the Fingerprint type that is part of this object as string.
	 * 
	 * @return Fingerprint type or null, if not set.
	 */
	String getFingerprintType();

	/**
	 * Returns the Fingerprint type that is part of this object as FingerprintType
	 * object known in RDKit.
	 * 
	 * @return Fingerprint type or null, if not set or not available.
	 */
	FingerprintType getRdkitFingerprintType();

	/**
	 * Returns the Torsion path length setting if set or {@link #UNAVAILABLE} if not set.
	 * 
	 * @return the Torsion path length value or {@link #UNAVAILABLE}.
	 */
	int getTorsionPathLength();

	/**
	 * Returns the minimum path setting if set or {@link #UNAVAILABLE} if not set.
	 * 
	 * @return the MinPath value or {@link #UNAVAILABLE}.
	 */
	int getMinPath();

	/**
	 * Returns the maximum path setting if set or {@link #UNAVAILABLE} if not set.
	 * 
	 * @return the MaxPath value or {@link #UNAVAILABLE}.
	 */
	int getMaxPath();

	/**
	 * Returns the AtomPair minimum path setting if set or {@link #UNAVAILABLE} if not set.
	 * 
	 * @return the AtomPair MinPath value or {@link #UNAVAILABLE}.
	 */
	int getAtomPairMinPath();

	/**
	 * Returns the AtomPair maximum path setting if set or {@link #UNAVAILABLE} if not set.
	 * 
	 * @return the AtomPair MaxPath value or {@link #UNAVAILABLE}.
	 */
	int getAtomPairMaxPath();

	/**
	 * Returns the number of bits (fingerprint length) if set or {@link #UNAVAILABLE} if not set.
	 * 
	 * @return the NumBits (length) value or {@link #UNAVAILABLE}.
	 */
	int getNumBits();

	/**
	 * Returns the radius setting if set or {@link #UNAVAILABLE} if not set.
	 * 
	 * @return the Radius value or {@link #UNAVAILABLE}.
	 */
	int getRadius();

	/**
	 * Returns the layer flags setting if set or {@link #UNAVAILABLE} if not set.
	 * 
	 * @return the Layer Flags value or {@link #UNAVAILABLE}.
	 */
	int getLayerFlags();

	/**
	 * Returns the Avalon query flag setting if set or {@link #UNAVAILABLE} if not set.
	 * 
	 * @return the Avalon query flag value (1) or (0) or {@link #UNAVAILABLE}.
	 */
	int getAvalonQueryFlag();

	/**
	 * Returns the Avalon bits flags setting if set or {@link #UNAVAILABLE} if not set.
	 * 
	 * @return the Avalon bit flags value or {@link #UNAVAILABLE}.
	 */
	int getAvalonBitFlags();

	/**
	 * Sets the fingerprint type and also the RDKit Fingerprint Type based on it,
	 * if known.
	 * 
	 * @param strType Fingerprint type.
	 * 
	 * @return A reference to this object. Makes it easy to concatenate settings calls.
	 */
	FingerprintSettings setFingerprintType(final String strType);

	/**
	 * Sets the RDKit Fingerprint type and also the normal string type based on it.
	 * 
	 * @param type
	 * 
	 * @return A reference to this object. Makes it easy to concatenate settings calls.
	 */
	FingerprintSettings setRDKitFingerprintType(final FingerprintType type);

	/**
	 * Sets the Torsion path length setting or {@link #UNAVAILABLE}.
	 * 
	 * @param iTorsionPathLength the TorsionPathLength value or {@link #UNAVAILABLE}.
	 * 
	 * @return A reference to this object. Makes it easy to concatenate settings calls.
	 */
	FingerprintSettings setTorsionPathLength(final int iTorsionPathLength);

	/**
	 * Sets the minimum path setting or {@link #UNAVAILABLE}.
	 * 
	 * @param iMinPath the MinPath value or {@link #UNAVAILABLE}.
	 * 
	 * @return A reference to this object. Makes it easy to concatenate settings calls.
	 */
	FingerprintSettings setMinPath(final int iMinPath);

	/**
	 * Sets the maximum path setting or {@link #UNAVAILABLE}.
	 * 
	 * @param iMaxPath the MaxPath value or {@link #UNAVAILABLE}.
	 * 
	 * @return A reference to this object. Makes it easy to concatenate settings calls.
	 */
	FingerprintSettings setMaxPath(final int iMaxPath);

	/**
	 * Sets the Atom Pairs minimum path setting or {@link #UNAVAILABLE}.
	 * 
	 * @param iMinPath the MinPath value or {@link #UNAVAILABLE}.
	 * 
	 * @return A reference to this object. Makes it easy to concatenate settings calls.
	 */
	FingerprintSettings setAtomPairMinPath(final int iMinPath);

	/**
	 * Sets the Atom Pairs maximum path setting or {@link #UNAVAILABLE}.
	 * 
	 * @param iMaxPath the MaxPath value or {@link #UNAVAILABLE}.
	 * 
	 * @return A reference to this object. Makes it easy to concatenate settings calls.
	 */
	FingerprintSettings setAtomPairMaxPath(final int iMaxPath);

	/**
	 * Sets the number of bits (fingerprint length) or {@link #UNAVAILABLE}.
	 * 
	 * @param iNumBits the NumBits (length) value or {@link #UNAVAILABLE}.
	 * 
	 * @return A reference to this object. Makes it easy to concatenate settings calls.
	 */
	FingerprintSettings setNumBits(final int iNumBits);

	/**
	 * Sets the radius setting or {@link #UNAVAILABLE}.
	 * 
	 * @param iRadius the Radius value or {@link #UNAVAILABLE}.
	 * 
	 * @return A reference to this object. Makes it easy to concatenate settings calls.
	 */
	FingerprintSettings setRadius(final int iRadius);

	/**
	 * Sets the layer flags setting or {@link #UNAVAILABLE}.
	 * 
	 * @param iLayerFlags the Layer Flags value or {@link #UNAVAILABLE}.
	 * 
	 * @return A reference to this object. Makes it easy to concatenate settings calls.
	 */
	FingerprintSettings setLayerFlags(final int iLayerFlags);

	/**
	 * Sets the Avalon query flag setting or {@link #UNAVAILABLE}.
	 * 
	 * @param iAvalonQueryFlag the Avalon query flag value or {@link #UNAVAILABLE}.
	 * 
	 * @return A reference to this object. Makes it easy to concatenate settings calls.
	 */
	FingerprintSettings setAvalonQueryFlag(final int iAvalonQueryFlag);

	/**
	 * Sets the Avalon bit flags setting or {@link #UNAVAILABLE}.
	 * 
	 * @param iAvalonBitFlags the Avalon bit flags or {@link #UNAVAILABLE}.
	 * 
	 * @return A reference to this object. Makes it easy to concatenate settings calls.
	 */
	FingerprintSettings setAvalonBitFlags(final int iAvalonBitFlags);

	/**
	 * Returns true, if the specified number is a value that is not equal
	 * to the value the represents an unavailable value.
	 * 
	 * @param iNumber A number to check.
	 * 
	 * @return True, if this value does represent a valid number. False,
	 * 		if it represents the reserved UNAVAILABLE value.
	 */
	boolean isAvailable(final int iNumber);

}
