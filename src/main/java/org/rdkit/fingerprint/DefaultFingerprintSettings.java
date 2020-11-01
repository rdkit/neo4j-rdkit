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

import org.RDKit.RDKFuncs;

import java.util.Map;
import java.util.logging.Logger;

/**
 * This class is the default implementation of the FingerprintSettings interface.
 * 
 * @author Manuel Schwarze
 */
public class DefaultFingerprintSettings implements FingerprintSettings {

	//
	// Constants
	//

	/** The logging instance. */
	private static final Logger LOGGER = Logger.getLogger(DefaultFingerprintSettings.class.getName());

	//
	// Constants
	//

	/** The default torsion path length to be used. */
	public static int DEFAULT_TORSION_PATH_LENGTH = 4;

	/** The default min path to be used. */
	public static int DEFAULT_MIN_PATH = 1;

	/** The default max path to be used. */
	public static int DEFAULT_MAX_PATH = 7;

	/** The default AtomPair min path to be used. */
	public static int DEFAULT_ATOMPAIR_MIN_PATH = 1;

	/** The default AtomPair max path to be used. */
	public static int DEFAULT_ATOMPAIR_MAX_PATH = 30;

	/** The default radius to be used. */
	public static int DEFAULT_RADIUS = 2;

	/** The default number of bits to be used. */
	public static int DEFAULT_NUM_BITS = 1024;

	/** The default layer flags to be used. */
	public static int DEFAULT_LAYER_FLAGS = 0xFFFF;

	/** The default Avalon query flag to be used. */
	public static int DEFAULT_AVALON_QUERY_FLAG = 0;

	/** The default Avalon bit flags to be used. */
	public static int DEFAULT_AVALON_BIT_FLAGS = RDKFuncs.getAvalonSimilarityBits();

	//
	// Members
	//

	/** The Fingerprint type. Can be null. */
	private String m_strType;

	/** The RDKit Fingerprint type. Can be null if unknown or undefined. */
	private FingerprintType m_rdkitType;

	/** The Fingerprint Torsion path length. Can be -1 {@link #UNAVAILABLE}, if not set. */
	private int m_iTorsionPathLength;

	/** The Fingerprint minimum path. Can be -1 {@link #UNAVAILABLE}, if not set. */
	private int m_iMinPath;

	/** The Fingerprint maximum path. Can be -1 {@link #UNAVAILABLE}, if not set. */
	private int m_iMaxPath;

	/** The Fingerprint AtomPair minimum path. Can be -1 {@link #UNAVAILABLE}, if not set. */
	private int m_iAtomPairMinPath;

	/** The Fingerprint AtomPair maximum path. Can be -1 {@link #UNAVAILABLE}, if not set. */
	private int m_iAtomPairMaxPath;

	/** The Fingerprint length. Can be -1 {@link #UNAVAILABLE}, if not set. */
	private int m_iNumBits;

	/** The Fingerprint radius. Can be -1 {@link #UNAVAILABLE}, if not set. */
	private int m_iRadius;

	/** The Fingerprint layer flags. Can be -1 {@link #UNAVAILABLE}, if not set. */
	private int m_iLayerFlags;

	/** The Avalon query flag. Can be -1 {@link #UNAVAILABLE}, if not set. */
	private int m_iAvalonQueryFlag;

	/** The Avalon bit flags. Can be -1 {@link #UNAVAILABLE}, if not set. */
	private int m_iAvalonBitFlags;

	//
	// Constructor
	//

	/**
	 * Creates a new default fingerprint settings object for a certain fingerprint type.
	 * 
	 * @param fpType Fingerprint type. Must not be null.
	 */
	public DefaultFingerprintSettings(final FingerprintType fpType) {
		// The following code ensures that only settings that make sense for a fingerprint type are getting set
		this(fpType.getSpecification(
				DEFAULT_TORSION_PATH_LENGTH /** iTorsionPathLength */,
				DEFAULT_MIN_PATH /** iMinPath */,
				DEFAULT_MAX_PATH /** iMaxPath */,
				DEFAULT_ATOMPAIR_MIN_PATH /** iAtomPairMinPath */,
				DEFAULT_ATOMPAIR_MAX_PATH /** iAtomPairMaxPath */,
				DEFAULT_NUM_BITS /** iNumBits */,
				DEFAULT_RADIUS /** iRadius */,
				DEFAULT_LAYER_FLAGS /** iLayerFlags */,
				DEFAULT_AVALON_QUERY_FLAG /** iAvalonQueryFlag */,
				DEFAULT_AVALON_BIT_FLAGS /** iAvalonBitFlags */));
	}

	/**
	 * Creates a new fingerprint settings object with the specified fingerprint type and settings.
	 * 
	 * @param strType Fingerprint type value. Can be null.
	 * @param iTorsionPathLength Torsion min path values. Can be -1 ({@link #UNAVAILABLE}.
	 * @param iMinPath Min Path value. Can be -1 ({@link #UNAVAILABLE}.
	 * @param iMaxPath Min Path value. Can be -1 ({@link #UNAVAILABLE}.
	 * @param iAtomPairMinPath AtomPair Min Path value. Can be -1 ({@link #UNAVAILABLE}.
	 * @param iAtomPairMaxPath AtomPair Max Path value. Can be -1 ({@link #UNAVAILABLE}.
	 * @param iNumBits Num Bits (Length) value. Can be -1 ({@link #UNAVAILABLE}.
	 * @param iRadius Radius value. Can be -1 ({@link #UNAVAILABLE}.
	 * @param iLayerFlags Layer Flags value. Can be -1 ({@link #UNAVAILABLE}.
	 * @param iAvalonQueryFlag Avalon query flag. Can be -1 ({@link #UNAVAILABLE}.
	 * @param iAvalonBitFlags Avalon bit flags. Can be -1 ({@link #UNAVAILABLE}.
	 */
	public DefaultFingerprintSettings(final String strType, final int iTorsionPathLength, final int iMinPath,
			final int iMaxPath, final int iAtomPairMinPath, final int iAtomPairMaxPath,
			final int iNumBits, final int iRadius, final int iLayerFlags,
			final int iAvalonQueryFlag, final int iAvalonBitFlags) {
		m_strType = strType;
		m_rdkitType = FingerprintType.parseString(m_strType);
		m_iTorsionPathLength = iTorsionPathLength;
		m_iMinPath = iMinPath;
		m_iMaxPath = iMaxPath;
		m_iAtomPairMinPath = iAtomPairMinPath;
		m_iAtomPairMaxPath = iAtomPairMaxPath;
		m_iNumBits = iNumBits;
		m_iRadius = iRadius;
		m_iLayerFlags = iLayerFlags;
		m_iAvalonQueryFlag = iAvalonQueryFlag;
		m_iAvalonBitFlags = iAvalonBitFlags;
	}

	/**
	 * Creates a new fingerprint settings object based on an existing one.
	 * 
	 * @param existing Existing fingerprint settings object or null to start with empty values.
	 */
	public DefaultFingerprintSettings(final FingerprintSettings existing) {
		reset();

		if (existing != null) {
			m_strType = existing.getFingerprintType();
			m_rdkitType = existing.getRdkitFingerprintType();
			m_iTorsionPathLength = existing.getTorsionPathLength();
			m_iMinPath = existing.getMinPath();
			m_iMaxPath = existing.getMaxPath();
			m_iAtomPairMinPath = existing.getAtomPairMinPath();
			m_iAtomPairMaxPath = existing.getAtomPairMaxPath();
			m_iNumBits = existing.getNumBits();
			m_iRadius = existing.getRadius();
			m_iLayerFlags = existing.getLayerFlags();
			m_iAvalonQueryFlag = existing.getAvalonQueryFlag();
			m_iAvalonBitFlags = existing.getAvalonBitFlags();
		}
	}

	//
	// Public Methods
	//

	@Override
	public synchronized String getFingerprintType() {
		return m_strType;
	}

	@Override
	public synchronized FingerprintType getRdkitFingerprintType() {
		return m_rdkitType;
	}

	@Override
	public synchronized int getTorsionPathLength() {
		return m_iTorsionPathLength;
	}

	@Override
	public synchronized int getMinPath() {
		return m_iMinPath;
	}

	@Override
	public synchronized int getMaxPath() {
		return m_iMaxPath;
	}

	@Override
	public synchronized int getAtomPairMinPath() {
		return m_iAtomPairMinPath;
	}

	@Override
	public synchronized int getAtomPairMaxPath() {
		return m_iAtomPairMaxPath;
	}

	@Override
	public synchronized int getNumBits() {
		return m_iNumBits;
	}

	@Override
	public synchronized int getRadius() {
		return m_iRadius;
	}

	@Override
	public synchronized int getLayerFlags() {
		return m_iLayerFlags;
	}

	@Override
	public int getAvalonQueryFlag() {
		return m_iAvalonQueryFlag;
	}

	@Override
	public synchronized int getAvalonBitFlags() {
		return m_iAvalonBitFlags;
	}

	@Override
	public synchronized FingerprintSettings setFingerprintType(final String strType) {
		m_strType = strType;
		m_rdkitType = FingerprintType.parseString(m_strType);
		return this;
	}

	@Override
	public synchronized FingerprintSettings setRDKitFingerprintType(final FingerprintType type) {
		m_strType = type == null ? null : type.toString();
		m_rdkitType = type;
		return this;
	}

	@Override
	public synchronized FingerprintSettings setTorsionPathLength(final int iTorsionPathLength) {
		this.m_iTorsionPathLength = iTorsionPathLength;
		return this;
	}

	@Override
	public synchronized FingerprintSettings setMinPath(final int iMinPath) {
		this.m_iMinPath = iMinPath;
		return this;
	}

	@Override
	public synchronized FingerprintSettings setMaxPath(final int iMaxPath) {
		this.m_iMaxPath = iMaxPath;
		return this;
	}

	@Override
	public synchronized FingerprintSettings setAtomPairMinPath(final int iAtomPairMinPath) {
		this.m_iAtomPairMinPath = iAtomPairMinPath;
		return this;
	}

	@Override
	public synchronized FingerprintSettings setAtomPairMaxPath(final int iAtomPairMaxPath) {
		this.m_iAtomPairMaxPath = iAtomPairMaxPath;
		return this;
	}

	@Override
	public synchronized FingerprintSettings setNumBits(final int iNumBits) {
		this.m_iNumBits = iNumBits;
		return this;
	}

	@Override
	public synchronized FingerprintSettings setRadius(final int iRadius) {
		this.m_iRadius = iRadius;
		return this;
	}

	@Override
	public synchronized FingerprintSettings setLayerFlags(final int iLayerFlags) {
		this.m_iLayerFlags = iLayerFlags;
		return this;
	}

	@Override
	public synchronized FingerprintSettings setAvalonQueryFlag(final int avalonQueryFlag) {
		this.m_iAvalonQueryFlag = avalonQueryFlag;
		return this;
	}

	@Override
	public synchronized FingerprintSettings setAvalonBitFlags(final int iAvalonBitFlags) {
		this.m_iAvalonBitFlags = iAvalonBitFlags;
		return this;
	}

	@Override
	public synchronized boolean isAvailable(final int iNumber) {
		return iNumber != UNAVAILABLE;
	}

	public synchronized void reset() {
		m_strType = null;
		m_rdkitType = null;
		m_iTorsionPathLength = UNAVAILABLE;
		m_iMinPath = UNAVAILABLE;
		m_iMaxPath = UNAVAILABLE;
		m_iAtomPairMinPath = UNAVAILABLE;
		m_iAtomPairMaxPath = UNAVAILABLE;
		m_iNumBits = UNAVAILABLE;
		m_iRadius = UNAVAILABLE;
		m_iLayerFlags = UNAVAILABLE;
		m_iAvalonQueryFlag = UNAVAILABLE;
		m_iAvalonBitFlags = UNAVAILABLE;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + m_iLayerFlags;
		result = prime * result + m_iAvalonQueryFlag;
		result = prime * result + m_iAvalonBitFlags;
		result = prime * result + m_iTorsionPathLength;
		result = prime * result + m_iMaxPath;
		result = prime * result + m_iMinPath;
		result = prime * result + m_iAtomPairMaxPath;
		result = prime * result + m_iAtomPairMinPath;
		result = prime * result + m_iNumBits;
		result = prime * result + m_iRadius;
		result = prime * result
				+ ((m_strType == null) ? 0 : m_strType.hashCode());
		result = prime * result
				+ ((m_rdkitType == null) ? 0 : m_rdkitType.hashCode());
		return result;
	}

	public synchronized boolean equals(final FingerprintSettings objSettingsToCompare) {
		boolean bRet = false;

		if (objSettingsToCompare == this) {
			bRet = true;
		}
		else if (objSettingsToCompare instanceof FingerprintSettings) {
			final FingerprintSettings settingsToCompare = objSettingsToCompare;

			if (Utils.equals(this.m_strType, settingsToCompare.getFingerprintType()) &&
					Utils.equals(this.m_rdkitType, settingsToCompare.getRdkitFingerprintType()) &&
					this.m_iTorsionPathLength == settingsToCompare.getTorsionPathLength() &&
					this.m_iMinPath == settingsToCompare.getMinPath() &&
					this.m_iMaxPath == settingsToCompare.getMaxPath() &&
					this.m_iAtomPairMinPath == settingsToCompare.getAtomPairMinPath() &&
					this.m_iAtomPairMaxPath == settingsToCompare.getAtomPairMaxPath() &&
					this.m_iNumBits == settingsToCompare.getNumBits() &&
					this.m_iRadius == settingsToCompare.getRadius() &&
					this.m_iLayerFlags == settingsToCompare.getLayerFlags() &&
					this.m_iAvalonQueryFlag == settingsToCompare.getAvalonQueryFlag() &&
					this.m_iAvalonBitFlags == settingsToCompare.getAvalonBitFlags()) {
				bRet = true;
			}
		}

		return bRet;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();

		if (getRdkitFingerprintType() != null) {
			sb.append(getRdkitFingerprintType().toString()).append(" Fingerprint");
		}
		else if (getFingerprintType() != null) {
			sb.append(getFingerprintType()).append(" Fingerprint");
		}
		if (isAvailable(getTorsionPathLength())) {
			if (sb.length() > 0) {
				sb.append("\n");
			}
			sb.append("Torsion Path Length: ").append(getTorsionPathLength());
		}
		if (isAvailable(getMinPath())) {
			if (sb.length() > 0) {
				sb.append("\n");
			}
			sb.append("Min Path: ").append(getMinPath());
		}
		if (isAvailable(getMaxPath())) {
			if (sb.length() > 0) {
				sb.append("\n");
			}
			sb.append("Max Path: ").append(getMaxPath());
		}
		if (isAvailable(getAtomPairMinPath())) {
			if (sb.length() > 0) {
				sb.append("\n");
			}
			sb.append("AtomPair Min Path: ").append(getAtomPairMinPath());
		}
		if (isAvailable(getAtomPairMaxPath())) {
			if (sb.length() > 0) {
				sb.append("\n");
			}
			sb.append("AtomPair Max Path: ").append(getAtomPairMaxPath());
		}
		if (isAvailable(getNumBits())) {
			if (sb.length() > 0) {
				sb.append("\n");
			}
			sb.append("Num Bits: ").append(getNumBits());
		}
		if (isAvailable(getRadius())) {
			if (sb.length() > 0) {
				sb.append("\n");
			}
			sb.append("Radius: ").append(getRadius());
		}
		if (isAvailable(getLayerFlags())) {
			if (sb.length() > 0) {
				sb.append("\n");
			}
			sb.append("Layered Flags: ").append(getLayerFlags());
		}
		if (isAvailable(getAvalonQueryFlag())) {
			if (sb.length() > 0) {
				sb.append("\n");
			}
			sb.append("Avalon Query Flag: ").append(getAvalonQueryFlag());
		}
		if (isAvailable(getAvalonBitFlags())) {
			if (sb.length() > 0) {
				sb.append("\n");
			}
			sb.append("Avalon Bit Flags: ").append(getAvalonBitFlags());
		}

		return sb.length() == 0 ? null : sb.toString();
	}

	//
	// Protected Methods
	//

	protected int getInt(final Map<String, String> mapProps, final String strKey, final String strColumnName) {
		int iRet = UNAVAILABLE;

		if (mapProps != null && mapProps.containsKey(strKey))
			try {
				iRet = Integer.parseInt(mapProps.get(strKey));
			}
		catch (final Exception exc) {
			LOGGER.warning("Header property '" + strKey + "' in column '" +
					strColumnName + "' is not representing a valid integer value: "
					+ mapProps.get(strKey) + " cannot be parsed.");
		}

		return iRet;
	}

	protected boolean getBoolean(final Map<String, String> mapProps, final String strKey, final String strColumnName) {
		boolean bRet = false;

		if (mapProps != null && mapProps.containsKey(strKey))
			try {
				bRet = Boolean.parseBoolean(mapProps.get(strKey));
			}
		catch (final Exception exc) {
			LOGGER.warning("Header property '" + strKey + "' in column '" +
					strColumnName + "' is not representing a valid boolean value: "
					+ mapProps.get(strKey) + " cannot be parsed.");
		}

		return bRet;
	}
}
