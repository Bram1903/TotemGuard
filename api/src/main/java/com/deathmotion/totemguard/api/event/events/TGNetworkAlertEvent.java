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

import com.deathmotion.totemguard.api.check.CheckType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Fired for every check violation across the whole network, including violations detected on
 * other servers in a Redis-backed cluster. This is the network-wide counterpart to
 * {@link TGUserFlagEvent} and {@link TGUserPunishEvent}, which fire only for violations
 * detected on the local server.
 *
 * <p>Unlike the check-driven events, this event carries flat, immutable data rather than live
 * {@link com.deathmotion.totemguard.api.check.Check} and
 * {@link com.deathmotion.totemguard.api.user.TGUser} handles, because the violator may not be
 * connected to the server handling the event. It is intended for fleet-wide aggregation such as
 * a single Discord bot reporting alerts from the entire network.
 *
 * <p>The event fires once per violation on every node: on the node that detected it with
 * {@link #isRemote()} {@code false}, and on every other node in the cluster with
 * {@link #isRemote()} {@code true}. The node identity check in the broker guarantees the
 * detecting node never receives its own broadcast, so there is exactly one fire per node per
 * violation. This event is observational and cannot be cancelled. To suppress a violation, cancel
 * the local {@link TGUserFlagEvent} or {@link TGUserPunishEvent} instead.
 */
public interface TGNetworkAlertEvent extends TGEvent {

    /**
     * The unique id of the player who triggered the violation.
     *
     * @return the violator's UUID
     */
    @NotNull UUID getPlayerUuid();

    /**
     * The name of the player who triggered the violation, as known to the detecting server at the
     * time of detection.
     *
     * @return the violator's name
     */
    @NotNull String getPlayerName();

    /**
     * The stable name of the check that produced this violation, as declared by
     * {@code @CheckData(name = ...)}.
     *
     * @return the check name
     */
    @NotNull String getCheckName();

    /**
     * The category of the check that produced this violation.
     *
     * @return the check type, never {@code null} (an unrecognised type from a newer node resolves
     * to {@link CheckType#UNSPECIFIED})
     */
    @NotNull CheckType getCheckType();

    /**
     * The violator's total violation count for this check at the moment of detection.
     *
     * @return the violation count, always at least one
     */
    int getViolations();

    /**
     * The pre-rendered debug payload for this violation (already template-substituted), or
     * {@code null} when the check produced no debug text for this dispatch.
     *
     * @return the debug text, or {@code null}
     */
    @Nullable String getDebug();

    /**
     * The configured display name of the server that detected the violation. For a single-server
     * install this is always the local server.
     *
     * @return the originating server's display name
     */
    @NotNull String getServerName();

    /**
     * Whether this event represents a flag (a staff alert) or a punishment.
     *
     * @return the alert kind
     */
    @NotNull Kind getKind();

    /**
     * Whether the violation was detected on a different node and received over the cluster, rather
     * than detected on the server handling this event.
     *
     * @return {@code true} if relayed from another node, {@code false} if detected locally
     */
    boolean isRemote();

    /**
     * The wall-clock time the violation was detected on its originating server, in milliseconds
     * since the Unix epoch.
     *
     * @return the detection timestamp in epoch milliseconds
     */
    long getTimestamp();

    /**
     * Distinguishes a flag (staff alert) from a punishment within a {@link TGNetworkAlertEvent}.
     */
    enum Kind {

        /**
         * A staff alert raised because a check flagged the player.
         */
        FLAG,

        /**
         * A punishment issued against the player after enough violations accumulated.
         */
        PUNISHMENT
    }
}
