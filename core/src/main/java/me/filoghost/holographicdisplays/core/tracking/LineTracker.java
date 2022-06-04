/*
 * Copyright (C) filoghost and contributors
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package me.filoghost.holographicdisplays.core.tracking;

import me.filoghost.holographicdisplays.core.base.BaseHologramLine;
import me.filoghost.holographicdisplays.core.tick.CachedPlayer;
import me.filoghost.holographicdisplays.core.tick.TickClock;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.MustBeInvokedByOverriders;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class LineTracker<T extends Viewer> {

    private static final int MODIFY_VIEWERS_INTERVAL_TICKS = 20;

    private final TickClock tickClock;
    private final Map<Player, T> viewers;
    private final Viewers<T> iterableViewers;

    /**
     * Flag to indicate that the line has changed in some way and there could be the need to send update packets.
     */
    private boolean lineChanged;
    private int lastVisibilitySettingsVersion;

    protected LineTracker(TickClock tickClock) {
        this.tickClock = tickClock;
        this.viewers = new HashMap<>();
        this.iterableViewers = new DelegateViewers<>(viewers.values());
    }

    protected abstract BaseHologramLine getLine();

    final boolean shouldBeRemoved() {
        return getLine().isDeleted();
    }

    @MustBeInvokedByOverriders
    public void onRemoval() {
        resetViewersAndSendDestroyPackets();
    }

    public final void setLineChanged() {
        lineChanged = true;
    }

    @MustBeInvokedByOverriders
    protected void update(List<CachedPlayer> onlinePlayers, List<CachedPlayer> movedPlayers) {
        boolean sendChangesPackets = false;

        // First, detect the changes if the flag is on and set it off
        if (lineChanged) {
            lineChanged = false;
            detectChanges();
            sendChangesPackets = true;
        }

        if (hasViewers()) {
            boolean textChanged = updatePlaceholders();
            if (textChanged) {
                sendChangesPackets = true;
            }
        }

        // Then, send the changes (if any) to already tracked players
        if (sendChangesPackets && hasViewers()) {
            sendChangesPackets(iterableViewers);
        }

        // Finally, add/remove viewers sending them the full spawn/destroy packets
        modifyViewersAndSendPackets(onlinePlayers, movedPlayers);

        if (sendChangesPackets) {
            clearDetectedChanges();
        }
    }

    protected abstract void detectChanges();

    protected abstract void clearDetectedChanges();

    protected abstract boolean updatePlaceholders();

    private void modifyViewersAndSendPackets(List<CachedPlayer> onlinePlayers, List<CachedPlayer> movedPlayers) {
        if (!getLine().isInLoadedChunk()) {
            resetViewersAndSendDestroyPackets();
            return;
        }

        boolean checkAllPlayers = false;

        int visibilitySettingsVersion = getLine().getVisibilitySettings().getVersion();
        if (visibilitySettingsVersion != lastVisibilitySettingsVersion) {
            lastVisibilitySettingsVersion = visibilitySettingsVersion;
            checkAllPlayers = true;
        }

        // Add the identity hash code to avoid updating all the lines at the same time
        if ((tickClock.getCurrentTick() + hashCode()) % MODIFY_VIEWERS_INTERVAL_TICKS == 0) {
            checkAllPlayers = true;
        }

        List<CachedPlayer> playersToCheck;
        if (checkAllPlayers) {
            playersToCheck = onlinePlayers;
        } else {
            playersToCheck = movedPlayers;
        }

        // Lazy initialization
        MutableViewers<T> addedPlayers = null;
        MutableViewers<T> removedPlayers = null;

        // Micro-optimization, don't use for-each loop to avoid creating a new Iterator (method called frequently)
        int size = playersToCheck.size();
        for (int i = 0; i < size; i++) {
            CachedPlayer player = playersToCheck.get(i);
            Player bukkitPlayer = player.getBukkitPlayer();
            if (shouldTrackPlayer(player)) {
                if (!viewers.containsKey(bukkitPlayer)) {
                    T viewer = createViewer(player);
                    viewers.put(bukkitPlayer, viewer);
                    if (addedPlayers == null) {
                        addedPlayers = new MutableViewers<>();
                    }
                    addedPlayers.add(viewer);
                }
            } else {
                if (viewers.containsKey(bukkitPlayer)) {
                    T viewer = viewers.remove(bukkitPlayer);
                    if (removedPlayers == null) {
                        removedPlayers = new MutableViewers<>();
                    }
                    removedPlayers.add(viewer);
                }
            }
        }

        if (addedPlayers != null) {
            sendSpawnPackets(addedPlayers);
        }
        if (removedPlayers != null) {
            sendDestroyPackets(removedPlayers);
        }
    }

    protected abstract T createViewer(CachedPlayer cachedPlayer);

    protected abstract boolean shouldTrackPlayer(CachedPlayer cachedPlayer);

    protected final boolean hasViewers() {
        return !viewers.isEmpty();
    }

    protected final Collection<T> getViewers() {
        return viewers.values();
    }

    public final boolean isViewer(Player player) {
        return viewers.containsKey(player);
    }

    protected final void removeViewer(Player player) {
        viewers.remove(player);
    }

    protected final void resetViewersAndSendDestroyPackets() {
        if (!hasViewers()) {
            return;
        }

        sendDestroyPackets(iterableViewers);
        viewers.clear();
    }

    protected abstract void sendSpawnPackets(Viewers<T> viewers);

    protected abstract void sendDestroyPackets(Viewers<T> viewers);

    protected abstract void sendChangesPackets(Viewers<T> viewers);

}
