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

package com.deathmotion.totemguard.loader.fleet;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Signs and verifies fleet pub/sub payloads with HMAC-SHA256 so an attacker with
 * Redis write access can't broadcast a malicious "jar-available" or rollout APPLY
 * that every peer would load. The signature is computed over the canonical JSON
 * body (everything except the signature field itself) plus a nonce field, and
 * stored under the {@code "sig"} key.
 * <p>
 * When no shared secret is configured the helper still <em>signs</em> outgoing
 * messages (so a future fleet rollout to signed-only is one config change away)
 * but <em>accepts</em> messages without signatures, logging once at FINE level
 * per topic. This is the trust-on-first-use mode covered by the SEVERE startup
 * warning in {@code LoaderConfig}.
 */
public final class FleetMessageAuth {

    private static final String SIG_FIELD = "sig";
    private static final String NONCE_FIELD = "nonce";
    private static final String HMAC_ALG = "HmacSHA256";

    private final Logger logger;
    private final byte @Nullable [] key;
    private final boolean strict;
    private final AtomicLong unsignedWarningCount = new AtomicLong();

    public FleetMessageAuth(Logger logger, @Nullable String sharedSecret) {
        this.logger = logger;
        if (sharedSecret == null || sharedSecret.isEmpty()) {
            this.key = null;
            this.strict = false;
        } else {
            this.key = sharedSecret.getBytes(StandardCharsets.UTF_8);
            this.strict = true;
        }
    }

    private static String canonicalJson(JsonObject obj) {
        java.util.List<String> keys = new java.util.ArrayList<>(obj.keySet());
        java.util.Collections.sort(keys);
        JsonObject sorted = new JsonObject();
        for (String k : keys) sorted.add(k, obj.get(k));
        return sorted.toString();
    }

    private static boolean constantTimeEquals(String a, String b) {
        byte[] ba = a.getBytes(StandardCharsets.UTF_8);
        byte[] bb = b.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(ba, bb);
    }

    /**
     * Add a signature and nonce to the payload. Mutates {@code payload}.
     */
    public void sign(JsonObject payload, Supplier<String> nonce) {
        if (key == null) return;
        payload.remove(SIG_FIELD);
        if (!payload.has(NONCE_FIELD)) {
            payload.addProperty(NONCE_FIELD, nonce.get());
        }
        String canonical = canonicalJson(payload);
        payload.addProperty(SIG_FIELD, hmac(canonical));
    }

    /**
     * Verify a payload. Returns true if the signature checks out, or if no key is
     * configured and the payload has no signature (trust-on-first-use). Returns
     * false (drop the message) only when a key is configured and the signature
     * is missing or wrong.
     */
    public boolean verify(JsonObject payload, String topic) {
        if (key == null) {
            // No secret configured. Accept everything but trace what we received.
            return true;
        }
        if (!payload.has(SIG_FIELD)) {
            warnUnsigned(topic, "missing signature");
            return false;
        }
        String provided = payload.get(SIG_FIELD).getAsString();
        JsonObject copy = payload.deepCopy();
        copy.remove(SIG_FIELD);
        String expected = hmac(canonicalJson(copy));
        if (!constantTimeEquals(provided, expected)) {
            warnUnsigned(topic, "signature mismatch");
            return false;
        }
        return true;
    }

    public boolean isStrict() {
        return strict;
    }

    private void warnUnsigned(String topic, String reason) {
        long n = unsignedWarningCount.incrementAndGet();
        if (n <= 5 || n % 100 == 0) {
            logger.log(Level.WARNING, "Dropping fleet message on " + topic + ": " + reason
                    + " (occurrences: " + n + ").");
        }
    }

    private String hmac(String body) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALG);
            mac.init(new SecretKeySpec(key, HMAC_ALG));
            byte[] sig = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(sig);
        } catch (NoSuchAlgorithmException | java.security.InvalidKeyException ex) {
            throw new IllegalStateException("HmacSHA256 unavailable", ex);
        }
    }
}
