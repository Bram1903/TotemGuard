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

package com.deathmotion.totemguard.loader.command;

import com.deathmotion.totemguard.loader.core.LoaderCore;
import com.deathmotion.totemguard.loader.runtime.PluginRuntime;

import java.util.concurrent.Future;
import java.util.logging.Logger;

/**
 * Surface area shared between {@code TGLoaderPaper} and {@code TGLoaderFabric} that
 * {@link LoaderCommandService} needs to drive the command set. Both platforms own
 * the same bootstrap worker and the same lifecycle flags, so command handlers can
 * stay platform-agnostic by going through this interface.
 */
public interface LoaderApp {

    LoaderCore core();

    PluginRuntime runtime();

    Logger logger();

    boolean isDisabling();

    Future<?> submitBackground(Runnable task);

    void attemptStart(String versionOverride) throws Exception;

    LoaderCore.StageResult attemptStage(String versionOverride) throws Exception;

    void attemptApplyStaged() throws Exception;
}
