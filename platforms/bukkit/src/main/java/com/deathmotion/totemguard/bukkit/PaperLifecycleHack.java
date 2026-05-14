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

package com.deathmotion.totemguard.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.incendo.cloud.paper.LegacyPaperCommandManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.BooleanSupplier;
import java.util.logging.Logger;

final class PaperLifecycleHack {

    private static final String PLEM_FQN =
            "io.papermc.paper.plugin.lifecycle.event.PaperLifecycleEventManager";
    private static final String RUNNER_FQN =
            "io.papermc.paper.plugin.lifecycle.event.LifecycleEventRunner";
    private static final String LIFECYCLE_EVENTS_FQN =
            "io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents";
    private static final String PAPER_COMMANDS_FQN =
            "io.papermc.paper.command.brigadier.PaperCommands";
    private static final String CAUSE_FQN =
            "io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent$Cause";

    private PaperLifecycleHack() {
    }

    static boolean isLifecycleRegistrationClosed(JavaPlugin plugin) {
        try {
            Object mgr = plugin.getLifecycleManager();
            Field check = mgr.getClass().getField("registrationCheck");
            BooleanSupplier supplier = (BooleanSupplier) check.get(mgr);
            return !supplier.getAsBoolean();
        } catch (Throwable t) {
            return false;
        }
    }

    static boolean forceRegisterBrigadier(JavaPlugin plugin,
                                          LegacyPaperCommandManager<?> manager,
                                          Logger logger) {
        Object lifecycleMgr = plugin.getLifecycleManager();
        if (!PLEM_FQN.equals(lifecycleMgr.getClass().getName())) {
            logger.warning("Unexpected lifecycle manager type: " + lifecycleMgr.getClass().getName()
                    + ". Cannot force-register brigadier.");
            return false;
        }

        Object unsafe;
        long checkOffset;
        BooleanSupplier originalCheck;
        Method putObject;
        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field theUnsafe = unsafeClass.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            unsafe = theUnsafe.get(null);

            Class<?> plemClass = Class.forName(PLEM_FQN);
            Field checkField = plemClass.getField("registrationCheck");
            originalCheck = (BooleanSupplier) checkField.get(lifecycleMgr);

            Method offsetFor = unsafeClass.getMethod("objectFieldOffset", Field.class);
            checkOffset = (long) offsetFor.invoke(unsafe, checkField);
            putObject = unsafeClass.getMethod("putObject", Object.class, long.class, Object.class);
        } catch (Throwable t) {
            logger.warning("Failed to reflect Paper lifecycle internals: " + t);
            return false;
        }

        try {
            unregisterStaleHandlers(plugin, logger);

            putObject.invoke(unsafe, lifecycleMgr, checkOffset, (BooleanSupplier) () -> true);
            try {
                manager.registerBrigadier();
            } finally {
                putObject.invoke(unsafe, lifecycleMgr, checkOffset, originalCheck);
            }
            return true;
        } catch (Throwable t) {
            logger.warning("Failed to force-register brigadier via lifecycle reflection: " + t);
            return false;
        }
    }

    static void publishCommandsToBrigadier(JavaPlugin plugin, Logger logger) {
        Object paperCommands;
        try {
            Class<?> paperCommandsClass = Class.forName(PAPER_COMMANDS_FQN);
            paperCommands = paperCommandsClass.getField("INSTANCE").get(null);
        } catch (Throwable t) {
            logger.log(java.util.logging.Level.WARNING,
                    "PaperCommands.INSTANCE not accessible. Brigadier suggestions will refresh on the next datapack reload.",
                    t);
            return;
        }

        boolean reopened = false;
        try {
            try {
                paperCommands.getClass().getMethod("setValid").invoke(paperCommands);
                reopened = true;
            } catch (NoSuchMethodException ignored) {
                logger.fine("PaperCommands.setValid() not present; firing event anyway");
            }

            Class<?> runnerClass = Class.forName(RUNNER_FQN);
            Object runner = runnerClass.getField("INSTANCE").get(null);

            Class<?> lifecycleEventsClass = Class.forName(LIFECYCLE_EVENTS_FQN);
            Object commandsEvent = lifecycleEventsClass.getField("COMMANDS").get(null);

            @SuppressWarnings({"rawtypes", "unchecked"})
            Class<?> causeClass = Class.forName(CAUSE_FQN);
            @SuppressWarnings({"rawtypes", "unchecked"})
            Object reloadCause = Enum.valueOf((Class) causeClass, "RELOAD");

            Method call = null;
            for (Method m : runnerClass.getMethods()) {
                if (m.getName().equals("callReloadableRegistrarEvent") && m.getParameterCount() == 4) {
                    call = m;
                    break;
                }
            }
            if (call == null) {
                logger.warning("callReloadableRegistrarEvent not found. Brigadier suggestions "
                        + "will refresh on the next datapack reload.");
                return;
            }

            call.invoke(runner, commandsEvent, paperCommands, plugin.getClass(), reloadCause);
        } catch (java.lang.reflect.InvocationTargetException ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            logger.log(java.util.logging.Level.WARNING,
                    "COMMANDS lifecycle event firing failed. Brigadier suggestions will refresh on the next datapack reload.",
                    cause);
        } catch (Throwable t) {
            logger.log(java.util.logging.Level.WARNING,
                    "Could not fire COMMANDS lifecycle event manually. Brigadier suggestions will refresh on the next datapack reload.",
                    t);
        } finally {
            if (reopened) {
                try {
                    paperCommands.getClass().getMethod("invalidate").invoke(paperCommands);
                } catch (Throwable ignored) {
                    // Best-effort cleanup. If invalidate() is unavailable Paper will reset
                    // the flag on the next natural lifecycle.
                }
            }
        }

        try {
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    try {
                        online.updateCommands();
                    } catch (Throwable ignored) {
                    }
                }
            });
        } catch (Throwable ignored) {
        }
    }

    private static void unregisterStaleHandlers(JavaPlugin plugin, Logger logger) {
        try {
            Class<?> runnerClass = Class.forName(RUNNER_FQN);
            Object runner = runnerClass.getField("INSTANCE").get(null);
            Method unregister = runnerClass.getMethod("unregisterAllEventHandlersFor", Plugin.class);
            unregister.invoke(runner, plugin);
        } catch (Throwable t) {
            logger.fine("Could not unregister stale lifecycle handlers (continuing): " + t);
        }
    }

}
