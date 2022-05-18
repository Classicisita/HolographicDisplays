/*
 * Copyright (C) filoghost and contributors
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package me.filoghost.holographicdisplays.plugin.hologram.base;

import me.filoghost.fcommons.Preconditions;
import me.filoghost.holographicdisplays.api.beta.Position;
import me.filoghost.holographicdisplays.plugin.hologram.tracking.LineTrackerManager;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class BaseHologram extends BaseHologramComponent {

    private final HologramPosition hologramPosition;
    private final LineTrackerManager lineTrackerManager;

    public BaseHologram(Position position, LineTrackerManager lineTrackerManager) {
        this.hologramPosition = new HologramPosition(position);
        this.lineTrackerManager = lineTrackerManager;
    }

    public abstract BaseHologramLines<? extends EditableHologramLine> getLines();

    protected abstract boolean isVisibleTo(Player player);

    public abstract Plugin getCreatorPlugin();

    protected final LineTrackerManager getTrackerManager() {
        return lineTrackerManager;
    }

    @Override
    public final void setDeleted() {
        super.setDeleted();
        getLines().setDeleted();
    }

    public @NotNull Position getPosition() {
        return hologramPosition.getPosition();
    }

    public @Nullable World getWorldIfLoaded() {
        return hologramPosition.getWorldIfLoaded();
    }

    public void setPosition(@NotNull Location location) {
        Preconditions.notNull(location, "location");
        Preconditions.notNull(location.getWorld(), "location.getWorld()");
        setPosition(location.getWorld().getName(), location.getX(), location.getY(), location.getZ());
    }

    public void setPosition(@NotNull World world, double x, double y, double z) {
        Preconditions.notNull(world, "world");
        setPosition(world.getName(), x, y, z);
    }

    public void setPosition(@NotNull String worldName, double x, double y, double z) {
        Preconditions.notNull(worldName, "worldName");
        setPosition(Position.of(worldName, x, y, z));
    }

    public void setPosition(@NotNull Position position) {
        Preconditions.notNull(position, "position");
        checkNotDeleted();

        hologramPosition.set(position);
        getLines().updatePositions();
    }

    protected void onWorldLoad(World world) {
        hologramPosition.onWorldLoad(world);
    }

    protected void onWorldUnload(World world) {
        hologramPosition.onWorldUnload(world);
    }

    protected void onChunkLoad(Chunk chunk) {
        hologramPosition.onChunkLoad(chunk);
    }

    protected void onChunkUnload(Chunk chunk) {
        hologramPosition.onChunkUnload(chunk);
    }

    protected boolean isInLoadedChunk() {
        return hologramPosition.isChunkLoaded();
    }

    @Override
    public String toString() {
        return "Hologram{"
                + "position=" + hologramPosition.getPosition()
                + ", lines=" + getLines()
                + ", deleted=" + isDeleted()
                + "}";
    }

}
