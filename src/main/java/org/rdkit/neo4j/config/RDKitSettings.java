package org.rdkit.neo4j.config;

/*-
 * #%L
 * RDKit-Neo4j plugin
 * %%
 * Copyright (C) 2019 - 2020 RDKit
 * %%
 * Copyright (C) 2019 Evgeny Sorokin
 * @@ All Rights Reserved @@
 * This file is part of the RDKit Neo4J integration.
 * The contents are covered by the terms of the BSD license
 * which is included in the file LICENSE, found at the root
 * of the neo4j-rdkit source tree.
 * #L%
 */

import org.neo4j.configuration.SettingImpl;
import org.neo4j.configuration.SettingValueParsers;
import org.neo4j.configuration.SettingsDeclaration;
import org.neo4j.graphdb.config.Setting;

public class RDKitSettings implements SettingsDeclaration {
    public static final Setting<Boolean> indexSanitize = SettingImpl.newBuilder("server.rdkit.index.sanitize", SettingValueParsers.BOOL, true).build();
}
