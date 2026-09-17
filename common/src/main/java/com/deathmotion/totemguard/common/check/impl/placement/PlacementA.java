package com.deathmotion.totemguard.common.check.impl.placement;

import com.deathmotion.totemguard.api.check.CheckType;
import com.deathmotion.totemguard.common.check.CheckImpl;
import com.deathmotion.totemguard.common.check.annotations.CheckData;
import com.deathmotion.totemguard.common.check.type.PacketCheck;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;

@CheckData(description = "Suspicious placement hit coordinates", type = CheckType.PLACEMENT, experimental = true)
public final class PlacementA extends CheckImpl implements PacketCheck {

    private static final int SAMPLES_REQUIRED = 3;
    private static final long SAMPLE_WINDOW_MS = 60_000L;

    private final SampleWindow samples = new SampleWindow();

    public PlacementA(TGPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT || event.isCancelled()) return;

        WrapperPlayClientPlayerBlockPlacement packet = new WrapperPlayClientPlayerBlockPlacement(event);
        if (samples.record(event.getTimestamp(), packet.getCursorPosition(), packet.getFace(), packet.getInsideBlock().orElse(false))) {
            fail("samples={0},cursor=0/0/0,face={1}", SAMPLES_REQUIRED, packet.getFace());
        }
    }

    static final class SampleWindow {

        private final ArrayDeque<Long> samples = new ArrayDeque<>(SAMPLES_REQUIRED);
        private long lastTimestamp = Long.MIN_VALUE;

        boolean record(long timestamp, @Nullable Vector3f cursor, @Nullable BlockFace face, boolean insideBlock) {
            if (timestamp < lastTimestamp) samples.clear();
            lastTimestamp = timestamp;
            while (!samples.isEmpty() && timestamp - samples.peekFirst() > SAMPLE_WINDOW_MS) samples.removeFirst();

            if (insideBlock || !isContradictoryOrigin(cursor, face)) return false;

            samples.addLast(timestamp);
            if (samples.size() < SAMPLES_REQUIRED) return false;

            samples.clear();
            return true;
        }

        static boolean isContradictoryOrigin(@Nullable Vector3f cursor, @Nullable BlockFace face) {
            if (cursor == null || cursor.getX() != 0.0F || cursor.getY() != 0.0F || cursor.getZ() != 0.0F) return false;
            return face == BlockFace.UP || face == BlockFace.EAST || face == BlockFace.SOUTH;
        }
    }
}
