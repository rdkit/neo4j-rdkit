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
 * This exception is thrown if fingerprint settings are incorrect in a certain context.
 *
 * @author Manuel Schwarze
 */
public class InvalidFingerprintSettingsException extends Exception {

	//
	// Constants
	//

	/** Serialnumber */
	private static final long serialVersionUID = 1L;

	//
	// Public Methods
	//

	/**
	 * Constructs an <code>InvalidFingerprintSettingsException</code> with the specified
	 * detail message.
	 *
	 * @param s the detail message.
	 */
	public InvalidFingerprintSettingsException(final String s) {
		super(s);
	}


	/**
	 * Constructs an <code>InvalidFingerprintSettingsException</code> with the specified
	 * cause.
	 *
	 * @param cause the original cause of the exeception
	 */
	public InvalidFingerprintSettingsException(final Throwable cause) {
		super(cause);
	}


	/**
	 * Constructs an <code>InvalidFingerprintSettingsException</code> with the specified
	 * detail message and a cause.

	 * @param msg the detail message
	 * @param cause the root cause
	 */
	public InvalidFingerprintSettingsException(final String msg, final Throwable cause) {
		super(msg, cause);
	}
}
