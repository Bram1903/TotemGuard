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

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import org.incendo.cloud.Command;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.component.CommandComponent;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.fabric.FabricServerCommandManager;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

final class FabricLifecycleHack {

    private static final String LATE_CATCHER_FQN =
            "org.incendo.cloud.fabric.internal.LateRegistrationCatcher";
    private static final String SERVER_STARTING_FLAG = "serverStartingCalled";
    private static final String SERVER_HANDLER_FQN =
            "org.incendo.cloud.fabric.FabricCommandRegistrationHandler$Server";

    private FabricLifecycleHack() {
    }

    static <C> FabricServerCommandManager<C> createBypassingStartedCheck(
            ExecutionCoordinator<C> coordinator,
            SenderMapper<CommandSourceStack, C> senderMapper,
            Logger logger
    ) {
        Field flag;
        boolean original;
        try {
            Class<?> catcher = Class.forName(LATE_CATCHER_FQN);
            flag = catcher.getDeclaredField(SERVER_STARTING_FLAG);
            flag.setAccessible(true);
            original = flag.getBoolean(null);
        } catch (Throwable t) {
            logger.log(Level.FINE, "LateRegistrationCatcher unreachable", t);
            return new FabricServerCommandManager<>(coordinator, senderMapper);
        }

        if (!original) {
            return new FabricServerCommandManager<>(coordinator, senderMapper);
        }

        try {
            flag.setBoolean(null, false);
            return new FabricServerCommandManager<>(coordinator, senderMapper);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException(ex);
        } finally {
            try {
                flag.setBoolean(null, original);
            } catch (Throwable ignored) {
            }
        }
    }

    static void publishCommandsToDispatcher(FabricServerCommandManager<?> manager, Logger logger) {
        MinecraftServer server = FabricServerHolder.server();
        if (server == null) return;

        Object newHandler = manager.commandRegistrationHandler();
        if (newHandler == null) return;

        evictStaleServerLambdas(newHandler, logger);

        Set<String> names = collectCommandNamesIncludingAliases(manager);
        if (!names.isEmpty()) {
            removeDispatcherNodes(server.getCommands().getDispatcher(), names);
        }

        Method registerAll = findRegisterAllCommands(newHandler.getClass());
        if (registerAll == null) {
            logger.warning("Cloud's Server.registerAllCommands signature changed");
            return;
        }
        registerAll.setAccessible(true);

        CommandDispatcher<CommandSourceStack> dispatcher = server.getCommands().getDispatcher();
        CommandBuildContext buildContext = CommandBuildContext.simple(
                server.registryAccess(),
                server.getWorldData().enabledFeatures()
        );
        try {
            registerAll.invoke(newHandler, dispatcher, buildContext, Commands.CommandSelection.DEDICATED);
        } catch (Throwable t) {
            logger.log(Level.WARNING, "Cloud registerAllCommands threw", t);
            return;
        }

        try {
            server.execute(() -> server.getPlayerList().getPlayers()
                    .forEach(p -> server.getCommands().sendCommands(p)));
        } catch (Throwable ignored) {
        }
    }

    private static Set<String> collectCommandNamesIncludingAliases(FabricServerCommandManager<?> manager) {
        Set<String> names = new HashSet<>();
        for (Command<?> cmd : manager.commands()) {
            CommandComponent<?> root = cmd.rootComponent();
            names.add(root.name());
            Collection<String> aliases = root.aliases();
            if (aliases != null) names.addAll(aliases);
        }
        return names;
    }

    private static Method findRegisterAllCommands(Class<?> type) {
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.getName().equals("registerAllCommands") && m.getParameterCount() == 3) {
                    return m;
                }
            }
        }
        return null;
    }

    private static void removeDispatcherNodes(CommandDispatcher<CommandSourceStack> dispatcher,
                                              Set<String> names) {
        CommandNode<CommandSourceStack> root = dispatcher.getRoot();
        for (String name : names) {
            removeChildByName(root, name);
        }
    }

    @SuppressWarnings("unchecked")
    private static void removeChildByName(CommandNode<?> root, String name) {
        try {
            Field childrenField = CommandNode.class.getDeclaredField("children");
            Field literalsField = CommandNode.class.getDeclaredField("literals");
            Field argumentsField = CommandNode.class.getDeclaredField("arguments");
            childrenField.setAccessible(true);
            literalsField.setAccessible(true);
            argumentsField.setAccessible(true);
            ((Map<String, ?>) childrenField.get(root)).remove(name);
            ((Map<String, ?>) literalsField.get(root)).remove(name);
            ((Map<String, ?>) argumentsField.get(root)).remove(name);
        } catch (Throwable ignored) {
        }
    }

    private static void evictStaleServerLambdas(Object keepServer, Logger logger) {
        Object event = CommandRegistrationCallback.EVENT;
        Class<?> serverType;
        try {
            serverType = Class.forName(SERVER_HANDLER_FQN);
        } catch (Throwable ignored) {
            return;
        }

        try {
            Field phasesField = findField(event.getClass(), "phases");
            Method updateMethod = findMethodNoArgs(event.getClass(), "update");
            if (phasesField == null || updateMethod == null) return;
            phasesField.setAccessible(true);
            updateMethod.setAccessible(true);

            Map<?, ?> phases = (Map<?, ?>) phasesField.get(event);
            for (Object phaseData : phases.values()) {
                Field listenersField = findField(phaseData.getClass(), "listeners");
                if (listenersField == null) continue;
                listenersField.setAccessible(true);
                Object listenersArray = listenersField.get(phaseData);
                int len = Array.getLength(listenersArray);
                Class<?> componentType = listenersArray.getClass().getComponentType();
                int kept = 0;
                Object retained = Array.newInstance(componentType, len);
                for (int i = 0; i < len; i++) {
                    Object listener = Array.get(listenersArray, i);
                    Object captured = extractCapturedServer(listener, serverType);
                    if (captured != null && captured != keepServer) continue;
                    Array.set(retained, kept++, listener);
                }
                Object trimmed = Array.newInstance(componentType, kept);
                System.arraycopy(retained, 0, trimmed, 0, kept);
                listenersField.set(phaseData, trimmed);
            }
            updateMethod.invoke(event);
        } catch (Throwable t) {
            logger.log(Level.WARNING, "Failed to purge stale Cloud Server lambdas", t);
        }
    }

    private static Object extractCapturedServer(Object listener, Class<?> serverType) {
        if (listener == null) return null;
        if (serverType.isInstance(listener)) return listener;
        for (Field f : listener.getClass().getDeclaredFields()) {
            try {
                f.setAccessible(true);
                Object value = f.get(listener);
                if (serverType.isInstance(value)) return value;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static Field findField(Class<?> type, String name) {
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
            }
        }
        return null;
    }

    private static Method findMethodNoArgs(Class<?> type, String name) {
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.getName().equals(name) && m.getParameterCount() == 0) {
                    return m;
                }
            }
        }
        return null;
    }
}
