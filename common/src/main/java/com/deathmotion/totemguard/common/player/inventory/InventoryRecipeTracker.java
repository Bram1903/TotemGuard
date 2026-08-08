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
import com.github.retrooper.packetevents.protocol.recipe.category.RecipeBookCategories;
import com.github.retrooper.packetevents.protocol.recipe.category.RecipeBookCategory;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSetDisplayedRecipe;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerRecipeBookAdd;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerRecipeBookRemove;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerRecipeBookSettings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public final class InventoryRecipeTracker {

    private static final AtomicInteger NEXT_RECIPE_ID = new AtomicInteger(ThreadLocalRandom.current().nextInt(10_000, 1_000_000));

    private static final int MAX_PRIOR_EQUIPMENT_COLLECTIONS = 19;

    private final TGPlayer player;
    private final Map<Integer, RecipeBookCategory> knownRecipeCategory = new HashMap<>();
    private final Map<Integer, Integer> knownRecipeGroup = new HashMap<>();
    private final Map<Integer, Integer> equipmentGroupSizes = new HashMap<>();
    private boolean knowsCraftingBookState;
    private boolean craftingBookOpen;
    private boolean craftingBookFiltering;
    private RecipeDisplayId recipeId;
    private boolean armed;
    private boolean addPending;
    private int equipmentUngroupedCount;

    public InventoryRecipeTracker(TGPlayer player) {
        this.player = player;
    }

    public boolean isAwaitingVisualConfirmation() {
        return armed && recipeId != null;
    }

    public void recordClientState(BookType type, boolean open, boolean filtering) {
        if (type != BookType.CRAFTING) {
            return;
        }

        this.knowsCraftingBookState = true;
        this.craftingBookOpen = open;
        this.craftingBookFiltering = filtering;
        if (open) {
            arm();
        } else {
            clearRecipe();
        }
    }

    public void recordServerSettings(RecipeBookSettings settings) {
        RecipeBookSettings.TypeState state = settings.getState(RecipeBookType.CRAFTING);
        this.knowsCraftingBookState = true;
        this.craftingBookOpen = state.isOpen();
        this.craftingBookFiltering = state.isFiltering();
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
        WrapperPlayServerRecipeBookAdd packet = new WrapperPlayServerRecipeBookAdd(event);
        final boolean replace = packet.isReplace();
        final List<WrapperPlayServerRecipeBookAdd.AddEntry> entries = packet.getEntries();

        final int probeId = recipeId == null ? -1 : recipeId.getId();
        boolean probeMatched = false;
        final List<TrackedEntry> snapshot = new ArrayList<>(entries.size());
        for (WrapperPlayServerRecipeBookAdd.AddEntry entry : entries) {
            RecipeDisplayEntry contents = entry.getContents();
            if (contents == null) continue;
            RecipeDisplayId id = contents.getId();
            if (id == null) continue;
            if (probeId != -1 && id.getId() == probeId) {
                probeMatched = true;
                continue;
            }
            snapshot.add(new TrackedEntry(id.getId(), contents.getCategory(), contents.getGroup()));
        }

        final boolean armProbe = probeMatched && addPending;
        player.getLatencyHandler().compensate(event, () -> {
            applyRecipeAdd(replace, snapshot);
            if (armProbe) {
                armRecipe(probeId);
            }
        });
    }

    public void handleRecipeRemove(PacketSendEvent event) {
        WrapperPlayServerRecipeBookRemove packet = new WrapperPlayServerRecipeBookRemove(event);
        final List<RecipeDisplayId> wireIds = packet.getRecipeIds();

        final int probeId = recipeId == null ? -1 : recipeId.getId();
        final List<Integer> ids = new ArrayList<>(wireIds.size());
        boolean probeMatched = false;
        for (RecipeDisplayId id : wireIds) {
            if (id == null) continue;
            int raw = id.getId();
            if (probeId != -1 && raw == probeId) {
                probeMatched = true;
            }
            ids.add(raw);
        }

        final boolean clearProbe = probeMatched;
        player.getLatencyHandler().compensate(event, () -> {
            applyRecipeRemove(ids);
            if (clearProbe) {
                clearRecipe(probeId);
            }
        });
    }

    private void applyRecipeAdd(boolean replace, List<TrackedEntry> entries) {
        if (replace) {
            knownRecipeCategory.clear();
            knownRecipeGroup.clear();
            equipmentUngroupedCount = 0;
            equipmentGroupSizes.clear();
            if (recipeId != null) {
                armed = false;
                addPending = false;
                recipeId = null;
            }
        }
        for (TrackedEntry entry : entries) {
            if (knownRecipeCategory.containsKey(entry.id())) continue;
            knownRecipeCategory.put(entry.id(), entry.category());
            if (entry.group() != null) {
                knownRecipeGroup.put(entry.id(), entry.group());
            }
            if (RecipeBookCategories.CRAFTING_EQUIPMENT.equals(entry.category())) {
                if (entry.group() == null) {
                    equipmentUngroupedCount++;
                } else {
                    equipmentGroupSizes.merge(entry.group(), 1, Integer::sum);
                }
            }
        }
    }

    private void applyRecipeRemove(List<Integer> ids) {
        for (int id : ids) {
            RecipeBookCategory category = knownRecipeCategory.remove(id);
            Integer group = knownRecipeGroup.remove(id);
            if (category == null) continue;
            if (RecipeBookCategories.CRAFTING_EQUIPMENT.equals(category)) {
                if (group == null) {
                    if (equipmentUngroupedCount > 0) equipmentUngroupedCount--;
                } else {
                    equipmentGroupSizes.compute(group, (k, v) -> v == null || v <= 1 ? null : v - 1);
                }
            }
        }
    }

    private int equipmentCollectionCount() {
        return equipmentUngroupedCount + equipmentGroupSizes.size();
    }

    private boolean supportsRecipeProbe() {
        return player.supportsEndTick();
    }

    private boolean shouldArm() {
        if (!supportsRecipeProbe()) return false;
        if (!knowsCraftingBookState || !craftingBookOpen) return false;
        if (craftingBookFiltering) return false;
        if (equipmentCollectionCount() > MAX_PRIOR_EQUIPMENT_COLLECTIONS) return false;
        return true;
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

    private record TrackedEntry(int id, RecipeBookCategory category, Integer group) {
    }
}
