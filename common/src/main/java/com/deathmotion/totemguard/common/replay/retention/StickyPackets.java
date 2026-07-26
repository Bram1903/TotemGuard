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

package com.deathmotion.totemguard.common.replay.retention;

import com.deathmotion.totemguard.common.replay.capture.PayloadReader;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;

import java.util.*;
import java.util.function.IntPredicate;

public final class StickyPackets {

    public static final int ORDER_JOIN = 0;
    public static final int ORDER_RESPAWN = 1;
    public static final int ORDER_PLAYER_STATE = 2;
    public static final int ORDER_CLIENT_STATE = 3;
    public static final int ORDER_INVENTORY = 4;
    public static final int ORDER_PLAYER_INFO = 5;
    public static final int ORDER_TEAMS = 6;
    public static final int ORDER_BORDER = 7;
    public static final int ORDER_ENTITY_ATTRIBUTES = 20;
    public static final int ORDER_ENTITY_METADATA = 21;
    public static final int ORDER_PASSENGERS = 22;
    public static final int ORDER_ENTITY_EFFECTS = 23;

    private static final int ACTION_START_SNEAKING = 0;
    private static final int ACTION_STOP_SNEAKING = 1;
    private static final int ACTION_LEAVE_BED = 2;
    private static final int ACTION_START_SPRINTING = 3;
    private static final int ACTION_STOP_SPRINTING = 4;
    private static final int ACTION_START_HORSE_JUMP = 5;
    private static final int ACTION_STOP_HORSE_JUMP = 6;
    private static final int ACTION_OPEN_HORSE_INVENTORY = 7;
    private static final int ACTION_START_ELYTRA_FLYING = 8;

    private static final int AXIS_SNEAK = 0;
    private static final int AXIS_BED = 1;
    private static final int AXIS_SPRINT = 2;
    private static final int AXIS_HORSE_JUMP = 3;
    private static final int AXIS_HORSE_INVENTORY = 4;
    private static final int AXIS_ELYTRA = 5;
    private static final int AXIS_UNKNOWN_BASE = 6;

    private static final Set<PacketTypeCommon> PLAYER_STATE = Set.of(
            PacketType.Play.Server.PLAYER_ABILITIES,
            PacketType.Play.Server.UPDATE_HEALTH,
            PacketType.Play.Server.SET_EXPERIENCE,
            PacketType.Play.Server.CHANGE_GAME_STATE,
            PacketType.Play.Server.HELD_ITEM_CHANGE,
            PacketType.Play.Server.CAMERA
    );

    private static final Set<PacketTypeCommon> BORDER = Set.of(
            PacketType.Play.Server.INITIALIZE_WORLD_BORDER,
            PacketType.Play.Server.WORLD_BORDER,
            PacketType.Play.Server.WORLD_BORDER_SIZE,
            PacketType.Play.Server.WORLD_BORDER_CENTER,
            PacketType.Play.Server.WORLD_BORDER_LERP_SIZE
    );

    private final Map<Key, Entry> entries = new LinkedHashMap<>();
    private final ArrayDeque<Entry> effects = new ArrayDeque<>();
    private final ArrayDeque<Entry> playerInfo = new ArrayDeque<>();

    private long sequence;
    private int bytes;
    private boolean saturated;
    private Key openWindow;

    private static int actionAxis(byte[] payload) {
        int action;
        try {
            PayloadReader reader = new PayloadReader(payload);
            reader.readVarInt();
            action = reader.readVarInt();
        } catch (RuntimeException malformed) {
            return -1;
        }
        return switch (action) {
            case ACTION_START_SNEAKING, ACTION_STOP_SNEAKING -> AXIS_SNEAK;
            case ACTION_LEAVE_BED -> AXIS_BED;
            case ACTION_START_SPRINTING, ACTION_STOP_SPRINTING -> AXIS_SPRINT;
            case ACTION_START_HORSE_JUMP, ACTION_STOP_HORSE_JUMP -> AXIS_HORSE_JUMP;
            case ACTION_OPEN_HORSE_INVENTORY -> AXIS_HORSE_INVENTORY;
            case ACTION_START_ELYTRA_FLYING -> AXIS_ELYTRA;
            default -> AXIS_UNKNOWN_BASE + action;
        };
    }

    private static int entityScopedOrder(PacketTypeCommon type) {
        if (type == PacketType.Play.Server.UPDATE_ATTRIBUTES) return ORDER_ENTITY_ATTRIBUTES;
        if (type == PacketType.Play.Server.ENTITY_METADATA) return ORDER_ENTITY_METADATA;
        if (type == PacketType.Play.Server.SET_PASSENGERS) return ORDER_PASSENGERS;
        return -1;
    }

    private static int firstVarInt(byte[] payload) {
        try {
            return new PayloadReader(payload).readVarInt();
        } catch (RuntimeException malformed) {
            return -1;
        }
    }

    public boolean saturated() {
        return saturated;
    }

    public int bytes() {
        return bytes;
    }

    public int size() {
        return entries.size() + effects.size() + playerInfo.size();
    }

    public synchronized void observeInbound(PacketTypeCommon type, int packetId, byte[] payload) {
        if (type == PacketType.Play.Client.ENTITY_ACTION) {
            int axis = actionAxis(payload);
            if (axis < 0) return;
            put(new Key(packetId, axis), ORDER_CLIENT_STATE, packetId, payload);
            return;
        }
        if (type == PacketType.Play.Client.PLAYER_ABILITIES
                || type == PacketType.Play.Client.HELD_ITEM_CHANGE) {
            put(new Key(packetId, 0L), ORDER_CLIENT_STATE, packetId, payload);
        }
    }

    public synchronized void observe(PacketTypeCommon type, int packetId, byte[] payload) {
        if (type == PacketType.Play.Server.JOIN_GAME) {
            reset();
            put(new Key(packetId, 0L), ORDER_JOIN, packetId, payload);
            return;
        }
        if (type == PacketType.Play.Server.RESPAWN) {
            dropEntityScoped();
            dropClientState();
            put(new Key(packetId, 0L), ORDER_RESPAWN, packetId, payload);
            return;
        }
        if (PLAYER_STATE.contains(type)) {
            put(new Key(packetId, 0L), ORDER_PLAYER_STATE, packetId, payload);
            return;
        }
        if (BORDER.contains(type)) {
            put(new Key(packetId, 0L), ORDER_BORDER, packetId, payload);
            return;
        }
        if (type == PacketType.Play.Server.TEAMS) {
            String team;
            try {
                team = new PayloadReader(payload).readString();
            } catch (RuntimeException malformed) {
                return;
            }
            put(new Key(packetId, team.hashCode() & 0xFFFFFFFFL), ORDER_TEAMS, packetId, payload);
            return;
        }
        if (type == PacketType.Play.Server.WINDOW_ITEMS) {
            put(new Key(packetId, 0L), ORDER_INVENTORY, packetId, payload);
            return;
        }
        if (type == PacketType.Play.Server.OPEN_WINDOW) {
            openWindow = new Key(packetId, 0L);
            put(openWindow, ORDER_INVENTORY, packetId, payload);
            return;
        }
        if (type == PacketType.Play.Server.CLOSE_WINDOW) {
            drop(openWindow);
            openWindow = null;
            return;
        }
        if (type == PacketType.Play.Server.PLAYER_INFO || type == PacketType.Play.Server.PLAYER_INFO_UPDATE) {
            append(playerInfo, RetentionPolicy.MAX_PLAYER_INFO_LOG, ORDER_PLAYER_INFO, packetId, payload, -1);
            return;
        }
        if (type == PacketType.Play.Server.ENTITY_EFFECT || type == PacketType.Play.Server.REMOVE_ENTITY_EFFECT) {
            append(effects, RetentionPolicy.MAX_EFFECT_LOG, ORDER_ENTITY_EFFECTS, packetId, payload,
                    firstVarInt(payload));
            return;
        }

        int order = entityScopedOrder(type);
        if (order < 0) return;
        int entityId = firstVarInt(payload);
        if (entityId < 0) return;
        put(new Key(packetId, entityId), order, packetId, payload);
    }

    private void put(Key key, int order, int packetId, byte[] payload) {
        Entry previous = entries.get(key);
        if (previous == null && (entries.size() >= RetentionPolicy.MAX_STICKY_ENTRIES
                || bytes + payload.length > RetentionPolicy.MAX_STICKY_BYTES)) {
            saturated = true;
            return;
        }
        if (previous != null) bytes -= previous.payload.length;
        bytes += payload.length;
        entries.put(key, new Entry(order, sequence++, packetId, payload, -1,
                order == ORDER_CLIENT_STATE));
    }

    private void drop(Key key) {
        if (key == null) return;
        Entry removed = entries.remove(key);
        if (removed != null) bytes -= removed.payload.length;
    }

    private void append(ArrayDeque<Entry> log, int cap, int order, int packetId, byte[] payload, int scope) {
        log.addLast(new Entry(order, sequence++, packetId, payload, scope));
        bytes += payload.length;
        while (log.size() > cap) {
            Entry dropped = log.removeFirst();
            bytes -= dropped.payload.length;
        }
    }

    private void dropClientState() {
        entries.entrySet().removeIf(entry -> {
            if (entry.getValue().order != ORDER_CLIENT_STATE) return false;
            bytes -= entry.getValue().payload.length;
            return true;
        });
    }

    private void dropEntityScoped() {
        entries.entrySet().removeIf(entry -> {
            if (entry.getValue().order < ORDER_ENTITY_ATTRIBUTES) return false;
            bytes -= entry.getValue().payload.length;
            return true;
        });
        for (Entry entry : effects) bytes -= entry.payload.length;
        effects.clear();
    }

    private void reset() {
        entries.clear();
        effects.clear();
        playerInfo.clear();
        openWindow = null;
        bytes = 0;
        saturated = false;
    }

    public synchronized void prune(IntPredicate stillTracked) {
        entries.entrySet().removeIf(entry -> {
            Key key = entry.getKey();
            if (entry.getValue().order < ORDER_ENTITY_ATTRIBUTES) return false;
            if (stillTracked.test((int) key.scope())) return false;
            bytes -= entry.getValue().payload.length;
            return true;
        });
        effects.removeIf(entry -> {
            if (entry.scope < 0 || stillTracked.test(entry.scope)) return false;
            bytes -= entry.payload.length;
            return true;
        });
    }

    public synchronized List<Entry> snapshot() {
        List<Entry> out = new ArrayList<>(entries.size() + effects.size() + playerInfo.size());
        out.addAll(entries.values());
        out.addAll(effects);
        out.addAll(playerInfo);
        out.sort(Comparator.<Entry>comparingInt(entry -> entry.order).thenComparingLong(entry -> entry.sequence));
        return out;
    }

    private record Key(int packetId, long scope) {
    }

    public static final class Entry {

        final int order;
        final long sequence;
        final int packetId;
        final byte[] payload;
        final int scope;
        final boolean inbound;

        Entry(int order, long sequence, int packetId, byte[] payload, int scope) {
            this(order, sequence, packetId, payload, scope, false);
        }

        Entry(int order, long sequence, int packetId, byte[] payload, int scope, boolean inbound) {
            this.order = order;
            this.sequence = sequence;
            this.packetId = packetId;
            this.payload = payload;
            this.scope = scope;
            this.inbound = inbound;
        }

        public int order() {
            return order;
        }

        public int packetId() {
            return packetId;
        }

        public byte[] payload() {
            return payload;
        }

        public boolean inbound() {
            return inbound;
        }
    }
}
