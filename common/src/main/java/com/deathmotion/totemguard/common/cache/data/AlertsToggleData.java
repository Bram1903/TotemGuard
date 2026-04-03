package com.deathmotion.totemguard.common.cache.data;

import com.deathmotion.totemguard.common.redis.cache.binary.RedisCodec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

public record AlertsToggleData(boolean alertsEnabled) implements RedisCodec<AlertsToggleData> {

    @Override
    public byte[] encode() throws Exception {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             DataOutputStream outputStream = new DataOutputStream(byteArrayOutputStream)) {

            outputStream.writeBoolean(this.alertsEnabled);
            outputStream.flush();
            return byteArrayOutputStream.toByteArray();
        }
    }

    @Override
    public AlertsToggleData decode(byte[] data) throws Exception {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
             DataInputStream inputStream = new DataInputStream(byteArrayInputStream)) {

            return new AlertsToggleData(inputStream.readBoolean());
        }
    }
}
