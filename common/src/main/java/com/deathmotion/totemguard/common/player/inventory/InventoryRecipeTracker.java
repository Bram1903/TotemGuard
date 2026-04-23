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

package com.deathmotion.totemguard.common.player.inventory;

import com.deathmotion.totemguard.common.player.TGPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.item.book.BookType;
import com.github.retrooper.packetevents.protocol.recipe.RecipeBookSettings;
import com.github.retrooper.packetevents.protocol.recipe.RecipeBookType;
import com.github.retrooper.packetevents.protocol.recipe.RecipeDisplayEntry;
import com.github.retrooper.packetevents.protocol.recipe.RecipeDisplayId;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSetDisplayedRecipe;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerRecipeBookAdd;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerRecipeBookRemove;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerRecipeBookSettings;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public final class InventoryRecipeTracker {

    private static final AtomicInteger NEXT_RECIPE_ID = new AtomicInteger(ThreadLocalRandom.current().nextInt(10_000, 1_000_000));

    private final TGPlayer player;
    private boolean knowsCraftingBookState;
    private boolean craftingBookOpen;
    private RecipeDisplayId recipeId;
    private boolean armed;
    private boolean addPending;

    public InventoryRecipeTracker(TGPlayer player) {
        this.player = player;
    }

    public boolean isAwaitingVisualConfirmation() {
        return armed && recipeId != null;
    }

    public void recordClientState(BookType type, boolean open) {
        if (type != BookType.CRAFTING) {
            return;
        }

        this.knowsCraftingBookState = true;
        this.craftingBookOpen = open;
        if (open) {
            arm();
        } else {
            clearRecipe();
        }
    }

    public void recordServerSettings(RecipeBookSettings settings) {
        this.knowsCraftingBookState = true;
        this.craftingBookOpen = settings.getState(RecipeBookType.CRAFTING).isOpen();
        if (craftingBookOpen) {
            arm();
        } else {
            clearRecipe();
        }
    }

    public void handleServerSettings(PacketSendEvent event) {
        WrapperPlayServerRecipeBookSettings packet = new WrapperPlayServerRecipeBookSettings(event);
        RecipeBookSettings settings = packet.getSettings();
        player.getLatencyHandler().compensate(event, () -> recordServerSettings(settings));
    }

    public boolean handleDisplayedRecipe(PacketReceiveEvent event) {
        if (!supportsRecipeProbe() || recipeId == null) {
            return false;
        }

        RecipeDisplayId seenRecipeId = new WrapperPlayClientSetDisplayedRecipe(event).getRecipeId();
        if (seenRecipeId == null || seenRecipeId.getId() != recipeId.getId()) {
            return false;
        }

        clearRecipe();
        return true;
    }

    public void armAfterClientClose() {
        if (!supportsRecipeProbe()) {
            return;
        }

        arm();
    }

    public void reset() {
        clearRecipe();
    }

    public void handleRecipeAdd(PacketSendEvent event) {
        if (recipeId == null || !addPending) {
            return;
        }

        WrapperPlayServerRecipeBookAdd packet = new WrapperPlayServerRecipeBookAdd(event);
        boolean matches = packet.getEntries().stream()
                .map(WrapperPlayServerRecipeBookAdd.AddEntry::getContents)
                .map(RecipeDisplayEntry::getId)
                .anyMatch(id -> id != null && id.getId() == recipeId.getId());

        if (!matches) {
            return;
        }

        int expectedId = recipeId.getId();
        player.getLatencyHandler().compensate(event, () -> armRecipe(expectedId));
    }

    public void handleRecipeRemove(PacketSendEvent event) {
        if (recipeId == null) {
            return;
        }

        WrapperPlayServerRecipeBookRemove packet = new WrapperPlayServerRecipeBookRemove(event);
        boolean matches = packet.getRecipeIds().stream()
                .anyMatch(id -> id != null && id.getId() == recipeId.getId());

        if (!matches) {
            return;
        }

        int expectedId = recipeId.getId();
        player.getLatencyHandler().compensate(event, () -> clearRecipe(expectedId));
    }

    private boolean supportsRecipeProbe() {
        return player.supportsEndTick();
    }

    private boolean shouldArm() {
        return supportsRecipeProbe()
                && knowsCraftingBookState
                && craftingBookOpen;
    }

    private void arm() {
        clearRecipe();

        if (!shouldArm()) {
            return;
        }

        this.recipeId = new RecipeDisplayId(NEXT_RECIPE_ID.incrementAndGet());
        this.armed = false;
        this.addPending = true;
        player.getUser().sendPacket(buildAddPacket(recipeId));
    }

    private void armRecipe(int expectedId) {
        if (recipeId == null || recipeId.getId() != expectedId || !addPending) {
            return;
        }

        addPending = false;
        armed = true;
    }

    private void clearRecipe() {
        if (recipeId == null) {
            return;
        }

        armed = false;
        addPending = false;
        player.getUser().sendPacket(new WrapperPlayServerRecipeBookRemove(List.of(recipeId)));
    }

    private void clearRecipe(int expectedId) {
        if (recipeId == null || recipeId.getId() != expectedId) {
            return;
        }

        recipeId = null;
        armed = false;
        addPending = false;
    }

    private WrapperPlayServerRecipeBookAdd buildAddPacket(RecipeDisplayId recipeId) {
        RecipeDisplayEntry entry = InventoryConstants.createRecipeProbeEntry(recipeId);
        WrapperPlayServerRecipeBookAdd.AddEntry addEntry = new WrapperPlayServerRecipeBookAdd.AddEntry(entry, false, true);
        return new WrapperPlayServerRecipeBookAdd(List.of(addEntry), false);
    }
}
