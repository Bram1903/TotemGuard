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

package com.deathmotion.totemguard.loader.download;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Single HttpClient shared by ArtifactDownloader, CachedJarStore, and the source
 * resolvers. JDK HttpClient is thread-safe and pools connections internally, so one
 * instance per JVM is correct. We also expose a small retry helper that classifies
 * transient network errors (connect/read timeout, IOException, 5xx) so a single
 * network blip during download or resolve doesn't surface as "hash mismatch" or a
 * confusing IOException to operators.
 */
public final class LoaderHttp {

    public static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    public static final Duration READ_TIMEOUT = Duration.ofSeconds(60);
    public static final int DEFAULT_RETRIES = 2;
    public static final Duration RETRY_BACKOFF = Duration.ofMillis(500);

    private static final HttpClient SHARED;

    static {
        AtomicInteger id = new AtomicInteger();
        ThreadFactory factory = r -> {
            Thread t = new Thread(r, "TotemGuard-Loader-Http-" + id.incrementAndGet());
            t.setDaemon(true);
            return t;
        };
        SHARED = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .executor(Executors.newCachedThreadPool(factory))
                .build();
    }

    private LoaderHttp() {
    }

    public static HttpClient client() {
        return SHARED;
    }

    public static boolean isTransient(Throwable t) {
        Throwable root = t;
        for (int hops = 0; hops < 32; hops++) {
            if (root instanceof Permanent) return false;
            Throwable next = root.getCause();
            if (next == null || next == root) break;
            root = next;
        }
        // Default: IOExceptions (timeouts, connect refused, partial reads, transient 5xx) are retryable.
        return t instanceof IOException;
    }

    public static <T> T retry(Logger logger, String label, IoAttempt<T> attempt) throws IOException {
        return retry(logger, label, DEFAULT_RETRIES, attempt, LoaderHttp::isTransient);
    }

    public static <T> T retry(Logger logger, String label, int retries,
                              IoAttempt<T> attempt, Predicate<Throwable> retryable) throws IOException {
        IOException last = null;
        for (int i = 0; i <= retries; i++) {
            try {
                return attempt.run();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IOException(label + " interrupted", ex);
            } catch (IOException ex) {
                last = ex;
                if (i == retries || !retryable.test(ex)) throw ex;
                long delay = RETRY_BACKOFF.toMillis() * (1L << i);
                logger.log(Level.FINE, label + " attempt " + (i + 1) + " failed ("
                        + ex.getClass().getSimpleName() + "). Retrying in " + delay + "ms.", ex);
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException interrupt) {
                    Thread.currentThread().interrupt();
                    throw new IOException(label + " interrupted during backoff", interrupt);
                }
            }
        }
        throw last == null ? new IOException(label + " failed") : last;
    }

    /**
     * Marker on exceptions the retry helper should not retry. Cause-chain checked, so
     * wrappers around a permanent error are still treated as permanent.
     */
    public interface Permanent {
    }

    @FunctionalInterface
    public interface IoAttempt<T> {
        T run() throws IOException, InterruptedException;
    }
}
