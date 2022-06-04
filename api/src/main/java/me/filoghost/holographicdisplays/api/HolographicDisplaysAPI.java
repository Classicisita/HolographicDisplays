/*
 * Copyright (C) filoghost and contributors
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package me.filoghost.holographicdisplays.api;

import me.filoghost.holographicdisplays.api.hologram.Hologram;
import me.filoghost.holographicdisplays.api.internal.HolographicDisplaysAPIProvider;
import me.filoghost.holographicdisplays.api.placeholder.GlobalPlaceholder;
import me.filoghost.holographicdisplays.api.placeholder.GlobalPlaceholderFactory;
import me.filoghost.holographicdisplays.api.placeholder.GlobalPlaceholderReplaceFunction;
import me.filoghost.holographicdisplays.api.placeholder.IndividualPlaceholder;
import me.filoghost.holographicdisplays.api.placeholder.IndividualPlaceholderFactory;
import me.filoghost.holographicdisplays.api.placeholder.IndividualPlaceholderReplaceFunction;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Main entry point for accessing the Holographic Displays API.
 *
 * @since 1
 */
public interface HolographicDisplaysAPI {

    /**
     * Returns the API version number, which is increased every time the API changes. This number is used to check if a
     * certain method or class (which may have been added later) is present or not.
     * <p>
     * All public API classes and methods are documented with the Javadoc tag {@code @since}, indicating in which API
     * version that element was introduced.
     * <p>
     * It can be used it to require a minimum version, as features may be added (rarely removed) in future versions. The
     * first version of the API is 1.
     * <p>
     * The API version is independent of the normal plugin version.
     *
     * @return the current API version
     * @since 1
     */
    static int getVersion() {
        return 1;
    }

    /**
     * Returns the API instance for managing holograms and placeholders of the specified plugin.
     * <p>
     * Holograms and placeholders created by other plugins are completely separate, each API instance can only manage
     * and retrieve the ones of the specified plugin. Unless for very specific reasons, a plugin should only use its own
     * API instance.
     *
     * @param plugin the plugin using the API
     * @return an API instance for the specified plugin
     * @since 1
     */
    static @NotNull HolographicDisplaysAPI get(@NotNull Plugin plugin) {
        return HolographicDisplaysAPIProvider.getImplementation().getHolographicDisplaysAPI(plugin);
    }

    /**
     * Creates a hologram.
     *
     * @param location the initial location of the hologram
     * @return the created hologram
     * @since 1
     */
    @NotNull Hologram createHologram(@NotNull Location location);

    /**
     * Creates a hologram.
     *
     * @param position the initial position of the hologram
     * @return the created hologram
     * @since 1
     */
    @NotNull Hologram createHologram(@NotNull Position position);

    /**
     * Returns all the valid holograms. A hologram is no longer valid after {@link Hologram#delete()} is invoked.
     *
     * @return an immutable collection of holograms
     * @since 1
     */
    @NotNull Collection<Hologram> getHolograms();

    /**
     * Deletes all the holograms.
     *
     * @since 1
     */
    void deleteHolograms();

    /**
     * @since 1
     */
    void registerGlobalPlaceholder(@NotNull String identifier, int refreshIntervalTicks, @NotNull GlobalPlaceholderReplaceFunction replaceFunction);

    /**
     * @since 1
     */
    void registerGlobalPlaceholder(@NotNull String identifier, @NotNull GlobalPlaceholder placeholder);

    /**
     * @since 1
     */
    void registerGlobalPlaceholderFactory(@NotNull String identifier, @NotNull GlobalPlaceholderFactory placeholderFactory);

    /**
     * @since 1
     */
    void registerIndividualPlaceholder(@NotNull String identifier, int refreshIntervalTicks, @NotNull IndividualPlaceholderReplaceFunction replaceFunction);

    /**
     * @since 1
     */
    void registerIndividualPlaceholder(@NotNull String identifier, @NotNull IndividualPlaceholder placeholder);

    /**
     * @since 1
     */
    void registerIndividualPlaceholderFactory(@NotNull String identifier, @NotNull IndividualPlaceholderFactory placeholderFactory);

    /**
     * @since 1
     */
    boolean isRegisteredPlaceholder(@NotNull String identifier);

    /**
     * Returns all the registered placeholder identifiers.
     *
     * @return a collection of placeholder identifiers
     * @since 1
     */
    @NotNull Collection<String> getRegisteredPlaceholders();

    /**
     * Unregisters a placeholder.
     *
     * @param identifier the identifier of the placeholder to remove
     * @since 1
     */
    void unregisterPlaceholder(@NotNull String identifier);

    /**
     * Resets and removes all the registered placeholders.
     * <p>
     * Can be useful to reset placeholders before registering all of them.
     *
     * @since 1
     */
    void unregisterPlaceholders();

}
