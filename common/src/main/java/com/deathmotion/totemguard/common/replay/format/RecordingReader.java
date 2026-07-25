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

package com.deathmotion.totemguard.common.replay.format;

import com.github.retrooper.packetevents.protocol.ConnectionState;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;

public final class RecordingReader implements AutoCloseable {

    private static final ConnectionState[] STATES = ConnectionState.values();
    private static final IntegrationInput[] INTEGRATION_INPUTS = IntegrationInput.values();

    private final DataInputStream in;
    private final PayloadDictionary dictionary = new PayloadDictionary();

    @Getter
    private final RecordingHeader header;

    private boolean sawEnd;
    private boolean hitEof;
    private long lastNanos;
    private long lastTick;

    public RecordingReader(Path source) throws IOException {
        InputStream file = Files.newInputStream(source);
        this.in = new DataInputStream(new GZIPInputStream(new BufferedInputStream(file, 1 << 16), 1 << 16));
        int magic = in.readInt();
        if (magic != ReplayFormat.MAGIC) {
            in.close();
            throw new IOException("Not a TotemGuard recording (bad magic)");
        }
        this.header = RecordingHeader.read(in);
        this.lastNanos = header.startNanos();
    }

    public boolean isTruncated() {
        return hitEof && !sawEnd;
    }

    public @Nullable RecordingFrame next() throws IOException {
        byte kind;
        try {
            kind = in.readByte();
        } catch (EOFException eof) {
            hitEof = true;
            return null;
        }
        long nanos = lastNanos + VarCodec.readSigned(in);
        lastNanos = nanos;
        switch (kind) {
            case ReplayFormat.FRAME_IN, ReplayFormat.FRAME_OUT -> {
                int flags = in.readUnsignedByte();
                ConnectionState state = STATES[flags & 0x7F];
                int packetId = VarCodec.readInt(in);
                byte[] payload = new byte[VarCodec.readInt(in)];
                in.readFully(payload);
                if (PayloadDictionary.eligible(payload)) dictionary.remember(payload);
                return new RecordingFrame.Packet(kind == ReplayFormat.FRAME_IN, (flags & 0x80) != 0,
                        state, packetId, nanos, payload);
            }
            case ReplayFormat.FRAME_IN_REPEAT, ReplayFormat.FRAME_OUT_REPEAT -> {
                int flags = in.readUnsignedByte();
                ConnectionState state = STATES[flags & 0x7F];
                int packetId = VarCodec.readInt(in);
                byte[] payload = dictionary.resolve(VarCodec.readInt(in));
                return new RecordingFrame.Packet(kind == ReplayFormat.FRAME_IN_REPEAT, (flags & 0x80) != 0,
                        state, packetId, nanos, payload);
            }
            case ReplayFormat.FRAME_TICK -> {
                lastTick += VarCodec.readSigned(in);
                return new RecordingFrame.ServerTick(nanos, lastTick);
            }
            case ReplayFormat.FRAME_LOOP -> {
                return new RecordingFrame.EventLoop(nanos);
            }
            case ReplayFormat.FRAME_VERDICT -> {
                return new RecordingFrame.Verdict(nanos, TickDigest.read(in));
            }
            case ReplayFormat.FRAME_MARK -> {
                return new RecordingFrame.Mark(nanos, in.readUTF());
            }
            case ReplayFormat.FRAME_PROLOGUE_END -> {
                return new RecordingFrame.PrologueEnd(nanos, WorldDigest.read(in));
            }
            case ReplayFormat.FRAME_FLAG -> {
                return new RecordingFrame.Flag(nanos, in.readUTF(), VarCodec.readInt(in), in.readUTF());
            }
            case ReplayFormat.FRAME_INTEGRATION -> {
                IntegrationInput input = INTEGRATION_INPUTS[in.readUnsignedByte()];
                return new RecordingFrame.Integration(nanos, input,
                        (int) VarCodec.readSigned(in), VarCodec.readSigned(in));
            }
            case ReplayFormat.FRAME_ATTACH -> {
                return new RecordingFrame.Attach(nanos);
            }
            case ReplayFormat.FRAME_END -> {
                sawEnd = true;
                return new RecordingFrame.End(nanos, RecordingTrailer.read(in));
            }
            default -> throw new IOException("Unknown frame kind " + kind);
        }
    }

    @Override
    public void close() throws IOException {
        in.close();
    }
}
