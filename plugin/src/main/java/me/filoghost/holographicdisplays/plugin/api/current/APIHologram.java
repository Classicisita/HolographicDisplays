/*
 * Copyright (C) filoghost and contributors
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package me.filoghost.holographicdisplays.plugin.api.current;

import me.filoghost.fcommons.Preconditions;
import me.filoghost.holographicdisplays.api.beta.Position;
import me.filoghost.holographicdisplays.api.beta.hologram.Hologram;
import me.filoghost.holographicdisplays.api.beta.hologram.PlaceholderSetting;
import me.filoghost.holographicdisplays.plugin.hologram.base.BaseHologram;
import me.filoghost.holographicdisplays.plugin.hologram.tracking.LineTrackerManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

class APIHologram extends BaseHologram implements Hologram {

    private final APIHologramLines lines;
    private final Plugin plugin;
    private final APIHologramManager hologramManager;
    private final DefaultVisibilitySettings visibilitySettings;

    private @NotNull PlaceholderSetting placeholderSetting;

    APIHologram(
            Position position,
            Plugin plugin,
            APIHologramManager hologramManager,
            LineTrackerManager lineTrackerManager) {
        super(position, lineTrackerManager);
        Preconditions.notNull(plugin, "plugin");
        this.lines = new APIHologramLines(this);
        this.plugin = plugin;
        this.hologramManager = hologramManager;
        this.visibilitySettings = new DefaultVisibilitySettings();
        this.placeholderSetting = PlaceholderSetting.DEFAULT;
    }

    @Override
    public @NotNull APIHologramLines getLines() {
        return lines;
    }

    @Override
    public @NotNull DefaultVisibilitySettings getVisibilitySettings() {
        return visibilitySettings;
    }

    @Override
    public @NotNull PlaceholderSetting getPlaceholderSetting() {
        return placeholderSetting;
    }

    @Override
    public void setPlaceholderSetting(@NotNull PlaceholderSetting placeholderSetting) {
        Preconditions.notNull(placeholderSetting, "placeholderSetting");
        checkNotDeleted();

        if (this.placeholderSetting == placeholderSetting) {
            return;
        }

        this.placeholderSetting = placeholderSetting;
        for (APIHologramLine line : lines) {
            line.setChanged();
        }
    }

    @Override
    public Plugin getCreatorPlugin() {
        return plugin;
    }

    @Override
    public boolean isVisibleTo(Player player) {
        return visibilitySettings.isVisibleTo(player);
    }

    @Override
    public void delete() {
        hologramManager.deleteHologram(this);
    }

}
