package com.deathmotion.totemguard.common.check.impl.mods;

import com.deathmotion.totemguard.api.check.CheckType;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.check.CheckData;
import com.deathmotion.totemguard.common.check.CheckImpl;
import com.deathmotion.totemguard.common.check.type.PacketCheck;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.util.NBTUtil;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.world.blockentity.BlockEntityTypes;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.configuration.client.WrapperConfigClientPluginMessage;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockEntityData;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBundle;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenSignEditor;
import net.kyori.adventure.text.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@CheckData(description = "Mod detection", type = CheckType.MOD)
public class Mod extends CheckImpl implements PacketCheck {

    private static final Logger LOGGER = Logger.getLogger(Mod.class.getName());

    private static final String REGISTER_CHANNEL = "minecraft:register";
    private static final int MAX_KEYS_PER_SIGN = 3;

    private static final List<ModSignature> SIGNATURES = List.of(
            new ModSignature(
                    "vanilla",
                    List.of("key.jump"),
                    List.of()
            ),
            new ModSignature(
                    "accurateblockplacement",
                    List.of("net.clayborn.accurateblockplacement.togglevanillaplacement"),
                    List.of()
            ),
            new ModSignature(
                    "autototem",
                    List.of(),
                    List.of("autototem")
            ),
            new ModSignature(
                    "tweakeroo",
                    List.of(),
                    List.of("servux:tweaks")
            )
    );

    private final ConcurrentHashMap<String, ModSignature> batchIdToSignature = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<String>> batchIdToKeys = new ConcurrentHashMap<>();

    private boolean isTriggered = false;

    public Mod(TGPlayer player) {
        super(player);
    }

    private void handle() {
        TGPlatform.getInstance().getScheduler().runAsyncTask(() -> {
            for (ModSignature sig : SIGNATURES) {
                List<String> keys = sig.translationKeys();
                if (keys == null || keys.isEmpty()) continue;

                int limit = Math.min(keys.size(), MAX_KEYS_PER_SIGN);
                if (keys.size() > MAX_KEYS_PER_SIGN) {
                    LOGGER.warning("[TotemGuard] ModSignature '" + sig.name() + "' has " + keys.size()
                            + " translation keys; only the first " + MAX_KEYS_PER_SIGN + " will be sent.");
                }

                List<Map.Entry<ModSignature, String>> batch = new ArrayList<>(limit);
                for (int i = 0; i < limit; i++) {
                    String key = keys.get(i);
                    if (key == null || key.isEmpty()) continue;
                    batch.add(Map.entry(sig, key));
                }
                if (batch.isEmpty()) continue;

                boolean wasSendingBundle = player.getData().isSendingBundlePacket();

                String batchId = UUID.randomUUID().toString();
                batchIdToSignature.put(batchId, sig);

                List<String> sentKeys = new ArrayList<>(batch.size());
                for (Map.Entry<ModSignature, String> e : batch) sentKeys.add(e.getValue());
                batchIdToKeys.put(batchId, List.copyOf(sentKeys));

                List<Component> lines = NBTUtil.buildLines(batchId, batch);
                NBTCompound signNbt = NBTUtil.buildSignNbt(lines, player.getClientVersion());

                WrapperPlayServerBundle bundle = new WrapperPlayServerBundle();

                Vector3i blockPosition = player.getLocation()
                        .getLocation()
                        .getPosition()
                        .toVector3i()
                        .add(0, 2, 0);

                WrappedBlockState signState = WrappedBlockState.getDefaultState(StateTypes.OAK_SIGN).clone();
                signState.setRotation(0);

                WrapperPlayServerBlockChange signPlace =
                        new WrapperPlayServerBlockChange(blockPosition, signState);

                WrapperPlayServerBlockEntityData modifySignData =
                        new WrapperPlayServerBlockEntityData(blockPosition, BlockEntityTypes.SIGN, signNbt);

                WrapperPlayServerOpenSignEditor openSignEditor =
                        new WrapperPlayServerOpenSignEditor(blockPosition, true);

                User user = player.getUser();
                if (!wasSendingBundle) user.sendPacket(bundle);
                user.sendPacket(signPlace);
                user.sendPacket(modifySignData);
                user.sendPacket(openSignEditor);
                if (!wasSendingBundle) user.sendPacket(bundle);
            }
        });
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();

        if (type == PacketType.Play.Client.PLUGIN_MESSAGE) {
            WrapperPlayClientPluginMessage packet = new WrapperPlayClientPluginMessage(event);
            handlePluginMessage(packet.getChannelName(), packet.getData());
        } else if (type == PacketType.Configuration.Client.PLUGIN_MESSAGE) {
            WrapperConfigClientPluginMessage packet = new WrapperConfigClientPluginMessage(event);
            handlePluginMessage(packet.getChannelName(), packet.getData());
        } else if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            if (isTriggered) return;
            isTriggered = true;
            TGPlatform.getInstance().getScheduler().runAsyncTaskDelayed(this::handle, 50, TimeUnit.MILLISECONDS);
        }
    }

    private void handlePluginMessage(String channel, byte[] data) {
        if (channel == null) return;

        String normalized = channel.toLowerCase();

        if (REGISTER_CHANNEL.equals(normalized)) {
            String payload = new String(data, StandardCharsets.UTF_8);
            for (String entry : payload.split("\0")) {
                checkKeywords(entry.toLowerCase());
            }
            return;
        }

        checkKeywords(normalized);
    }

    private void checkKeywords(String value) {
        for (ModSignature sig : SIGNATURES) {
            for (String keyword : sig.pluginMessageKeywords()) {
                if (keyword != null && !keyword.isEmpty() && value.contains(keyword.toLowerCase())) {
                    fail(sig.name());
                    return;
                }
            }
        }
    }
}
