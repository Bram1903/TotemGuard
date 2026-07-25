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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;

public final class RecordingWriter implements AutoCloseable {

    private static final int DEFLATE_BUFFER = 1 << 13;
    private static final int FILE_BUFFER = 1 << 16;

    private final DataOutputStream out;
    private final CountingStream counter;
    private final PayloadDictionary dictionary = new PayloadDictionary();

    private long lastNanos;
    private long lastTick;

    public RecordingWriter(Path target, RecordingHeader header) throws IOException {
        this(open(target), header);
    }

    public RecordingWriter(OutputStream sink, RecordingHeader header) throws IOException {
        this.counter = new CountingStream(new BufferedOutputStream(sink, FILE_BUFFER));
        this.out = new DataOutputStream(new GZIPOutputStream(counter, DEFLATE_BUFFER));
        out.writeInt(ReplayFormat.MAGIC);
        header.write(out);
        this.lastNanos = header.startNanos();
    }

    private static OutputStream open(Path target) throws IOException {
        Files.createDirectories(target.getParent());
        return Files.newOutputStream(target);
    }

    public long compressedBytes() {
        return counter.count;
    }

    public void write(RecordingFrame frame) throws IOException {
        if (frame instanceof RecordingFrame.Packet packet) {
            byte[] payload = packet.payload();
            int repeat = PayloadDictionary.eligible(payload) ? dictionary.lookup(payload) : -1;
            if (repeat >= 0) {
                head(packet.inbound() ? ReplayFormat.FRAME_IN_REPEAT : ReplayFormat.FRAME_OUT_REPEAT,
                        packet.nanos());
                out.writeByte(packet.state().ordinal() | (packet.cancelled() ? 0x80 : 0));
                VarCodec.writeInt(out, packet.packetId());
                VarCodec.writeInt(out, repeat);
                return;
            }
            head(packet.inbound() ? ReplayFormat.FRAME_IN : ReplayFormat.FRAME_OUT, packet.nanos());
            out.writeByte(packet.state().ordinal() | (packet.cancelled() ? 0x80 : 0));
            VarCodec.writeInt(out, packet.packetId());
            VarCodec.writeInt(out, payload.length);
            out.write(payload);
            if (PayloadDictionary.eligible(payload)) dictionary.remember(payload);
        } else if (frame instanceof RecordingFrame.ServerTick tick) {
            head(ReplayFormat.FRAME_TICK, tick.nanos());
            VarCodec.writeSigned(out, tick.tick() - lastTick);
            lastTick = tick.tick();
        } else if (frame instanceof RecordingFrame.EventLoop loop) {
            head(ReplayFormat.FRAME_LOOP, loop.nanos());
        } else if (frame instanceof RecordingFrame.Verdict verdict) {
            head(ReplayFormat.FRAME_VERDICT, verdict.nanos());
            verdict.digest().write(out);
        } else if (frame instanceof RecordingFrame.Mark mark) {
            head(ReplayFormat.FRAME_MARK, mark.nanos());
            out.writeUTF(mark.label());
        } else if (frame instanceof RecordingFrame.PrologueEnd prologue) {
            head(ReplayFormat.FRAME_PROLOGUE_END, prologue.nanos());
            prologue.digest().write(out);
        } else if (frame instanceof RecordingFrame.Flag flag) {
            head(ReplayFormat.FRAME_FLAG, flag.nanos());
            out.writeUTF(flag.check());
            VarCodec.writeInt(out, flag.violations());
            out.writeUTF(flag.debug());
        } else if (frame instanceof RecordingFrame.Integration integration) {
            head(ReplayFormat.FRAME_INTEGRATION, integration.nanos());
            out.writeByte(integration.input().ordinal());
            VarCodec.writeSigned(out, integration.id());
            VarCodec.writeSigned(out, integration.timestamp());
        } else if (frame instanceof RecordingFrame.Attach attach) {
            head(ReplayFormat.FRAME_ATTACH, attach.nanos());
        } else if (frame instanceof RecordingFrame.End end) {
            head(ReplayFormat.FRAME_END, end.nanos());
            end.trailer().write(out);
        } else {
            throw new IOException("Unknown frame " + frame.getClass().getName());
        }
    }

    private void head(byte kind, long nanos) throws IOException {
        out.writeByte(kind);
        VarCodec.writeSigned(out, nanos - lastNanos);
        lastNanos = nanos;
    }

    @Override
    public void close() throws IOException {
        out.close();
    }

    private static final class CountingStream extends FilterOutputStream {

        private volatile long count;

        private CountingStream(OutputStream delegate) {
            super(delegate);
        }

        @Override
        public void write(int value) throws IOException {
            out.write(value);
            count++;
        }

        @Override
        public void write(byte[] buffer, int offset, int length) throws IOException {
            out.write(buffer, offset, length);
            count += length;
        }
    }
}
