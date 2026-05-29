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

package com.deathmotion.totemguard.api.event.events;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fired when a TotemGuard subsystem reports an operational health change worth surfacing to
 * operators, such as the database becoming unreachable and later recovering, or the Redis link
 * dropping and reconnecting. It exists so monitoring integrations (for example a Discord bot
 * posting to an operations channel) can react to infrastructure problems the moment they happen
 * rather than waiting for an operator to read the server log.
 *
 * <p>Diagnostics are de-duplicated at the source: a subsystem fires one event when it transitions
 * to unhealthy and one when it recovers, rather than one per failed operation. In a Redis-backed
 * cluster the event is relayed to every node (with {@link #isRemote()} {@code true} on the
 * receivers), so a single monitoring node observes the whole network. A diagnostic about Redis
 * itself going down cannot be relayed (the transport it would use is the thing that failed) and so
 * stays local to the node that raised it.
 *
 * <p>This event is observational and cannot be cancelled.
 */
public interface TGDiagnosticEvent extends TGEvent {

    /**
     * The severity of this diagnostic, which a consumer can use to decide colouring, routing, or
     * whether to alert at all.
     *
     * @return the severity level
     */
    @NotNull Severity getSeverity();

    /**
     * A short human-readable label for the subsystem that raised the diagnostic, for example
     * {@code "Database"} or {@code "Redis"}. Intended for display, not for branching, branch on
     * {@link #getSeverity()} instead.
     *
     * @return the subsystem label
     */
    @NotNull String getSubsystem();

    /**
     * A human-readable description of what happened, suitable for showing directly to an operator.
     *
     * @return the diagnostic message
     */
    @NotNull String getMessage();

    /**
     * The stack trace of the throwable that triggered this diagnostic, rendered to a string, or
     * {@code null} when no throwable was involved (for example a clean recovery notice). A string
     * is used rather than a live {@link Throwable} so the diagnostic survives relaying across the
     * cluster.
     *
     * @return the rendered stack trace, or {@code null}
     */
    @Nullable String getStackTrace();

    /**
     * The configured display name of the server that raised the diagnostic.
     *
     * @return the originating server's display name
     */
    @NotNull String getServerName();

    /**
     * Whether this diagnostic was raised on a different node and received over the cluster, rather
     * than raised on the server handling this event.
     *
     * @return {@code true} if relayed from another node, {@code false} if raised locally
     */
    boolean isRemote();

    /**
     * The wall-clock time the diagnostic was raised on its originating server, in milliseconds
     * since the Unix epoch.
     *
     * @return the timestamp in epoch milliseconds
     */
    long getTimestamp();

    /**
     * The severity of a {@link TGDiagnosticEvent}, in increasing order of urgency.
     */
    enum Severity {

        /**
         * Informational, no action required. Typically a recovery notice.
         */
        INFO,

        /**
         * A degraded but non-fatal condition that an operator should be aware of.
         */
        WARNING,

        /**
         * A failure that impairs functionality, such as a feature being unavailable.
         */
        ERROR,

        /**
         * A severe failure that compromises core operation, such as the database being
         * unreachable while the server is live.
         */
        CRITICAL
    }
}
