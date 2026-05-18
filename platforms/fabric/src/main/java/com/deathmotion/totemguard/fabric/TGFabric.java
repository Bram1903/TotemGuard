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

package com.deathmotion.totemguard.fabric;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;

import java.text.MessageFormat;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public final class TGFabric implements DedicatedServerModInitializer {

    private TGFabricPlatform tg;

    private static void bridgeJulToFabricLog(String loggerName) {
        Logger jul = Logger.getLogger(loggerName);
        org.slf4j.Logger slf4j = org.slf4j.LoggerFactory.getLogger(loggerName);
        jul.setUseParentHandlers(false);
        for (Handler h : jul.getHandlers()) jul.removeHandler(h);
        jul.addHandler(new Slf4jBridgeHandler(slf4j));
        jul.setLevel(Level.ALL);
    }

    @Override
    public void onInitializeServer() {
        // Reroute the "TotemGuard" JUL logger through SLF4J before anything logs.
        // Paper auto-wires JUL into Log4j2 but Fabric does not, so without this the
        // default ConsoleHandler emits ugly two-line records.
        bridgeJulToFabricLog("TotemGuard");

        tg = new TGFabricPlatform(FabricLoader.getInstance().getConfigDir().resolve("totemguard"));

        ServerLifecycleEvents.SERVER_STARTED.register(_ -> tg.commonOnEnable());
        ServerLifecycleEvents.SERVER_STOPPING.register(_ -> tg.commonOnDisable());
    }

    public TGFabricPlatform getTg() {
        return tg;
    }

    private static final class Slf4jBridgeHandler extends Handler {
        private final org.slf4j.Logger target;

        Slf4jBridgeHandler(org.slf4j.Logger target) {
            this.target = target;
        }

        @Override
        public void publish(LogRecord r) {
            if (r == null) return;
            String msg = r.getMessage();
            Object[] params = r.getParameters();
            if (msg != null && params != null && params.length > 0) {
                try {
                    msg = MessageFormat.format(msg, params);
                } catch (IllegalArgumentException ignored) {
                }
            }
            Throwable t = r.getThrown();
            int lvl = r.getLevel().intValue();
            if (lvl >= Level.SEVERE.intValue()) target.error(msg, t);
            else if (lvl >= Level.WARNING.intValue()) target.warn(msg, t);
            else if (lvl >= Level.INFO.intValue()) target.info(msg, t);
            else if (lvl >= Level.FINE.intValue()) target.debug(msg, t);
            else target.trace(msg, t);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }
}
