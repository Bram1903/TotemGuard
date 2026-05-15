/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2026 Bram and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.deathmotion.totemguard.loader.source;

import java.net.URI;
import java.util.Map;

public record Artifact(
        String version,
        URI downloadUri,
        HashAlgorithm hashAlgorithm,
        String hashHex,
        String fileName,
        String sourceLabel,
        Map<String, String> headers
) {
    public Artifact(String version, URI downloadUri, HashAlgorithm hashAlgorithm,
                    String hashHex, String fileName, String sourceLabel) {
        this(version, downloadUri, hashAlgorithm, hashHex, fileName, sourceLabel, Map.of());
    }

    public enum HashAlgorithm {
        SHA_256("SHA-256"),
        SHA_512("SHA-512");

        private final String jdkName;

        HashAlgorithm(String jdkName) {
            this.jdkName = jdkName;
        }

        public String jdkName() {
            return jdkName;
        }
    }
}
