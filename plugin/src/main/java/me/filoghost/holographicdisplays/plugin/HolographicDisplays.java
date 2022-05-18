/*
 * Copyright (C) filoghost and contributors
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package me.filoghost.holographicdisplays.plugin;

import com.gmail.filoghost.holographicdisplays.api.internal.HologramsAPIProvider;
import me.filoghost.fcommons.FCommonsPlugin;
import me.filoghost.fcommons.FeatureSupport;
import me.filoghost.fcommons.logging.ErrorCollector;
import me.filoghost.holographicdisplays.api.beta.Position;
import me.filoghost.holographicdisplays.api.beta.hologram.Hologram;
import me.filoghost.holographicdisplays.api.beta.internal.HolographicDisplaysAPIProvider;
import me.filoghost.holographicdisplays.nms.common.NMSManager;
import me.filoghost.holographicdisplays.plugin.api.current.APIHologramManager;
import me.filoghost.holographicdisplays.plugin.api.current.DefaultHolographicDisplaysAPIProvider;
import me.filoghost.holographicdisplays.plugin.api.v2.V2HologramManager;
import me.filoghost.holographicdisplays.plugin.api.v2.V2HologramsAPIProvider;
import me.filoghost.holographicdisplays.plugin.bridge.bungeecord.BungeeServerTracker;
import me.filoghost.holographicdisplays.plugin.bridge.placeholderapi.PlaceholderAPIHook;
import me.filoghost.holographicdisplays.plugin.commands.HologramCommandManager;
import me.filoghost.holographicdisplays.plugin.commands.InternalHologramEditor;
import me.filoghost.holographicdisplays.plugin.config.ConfigManager;
import me.filoghost.holographicdisplays.plugin.config.InternalHologramConfig;
import me.filoghost.holographicdisplays.plugin.config.InternalHologramLoadException;
import me.filoghost.holographicdisplays.plugin.config.Settings;
import me.filoghost.holographicdisplays.plugin.config.upgrade.AnimationsLegacyUpgrade;
import me.filoghost.holographicdisplays.plugin.config.upgrade.DatabaseLegacyUpgrade;
import me.filoghost.holographicdisplays.plugin.config.upgrade.SymbolsLegacyUpgrade;
import me.filoghost.holographicdisplays.plugin.hologram.base.BaseHologram;
import me.filoghost.holographicdisplays.plugin.hologram.tracking.LineTrackerManager;
import me.filoghost.holographicdisplays.plugin.internal.hologram.InternalHologram;
import me.filoghost.holographicdisplays.plugin.internal.hologram.InternalHologramLine;
import me.filoghost.holographicdisplays.plugin.internal.hologram.InternalHologramManager;
import me.filoghost.holographicdisplays.plugin.internal.placeholder.AnimationPlaceholderFactory;
import me.filoghost.holographicdisplays.plugin.internal.placeholder.DefaultPlaceholders;
import me.filoghost.holographicdisplays.plugin.listener.ChunkListener;
import me.filoghost.holographicdisplays.plugin.listener.LineClickListener;
import me.filoghost.holographicdisplays.plugin.listener.PlayerListener;
import me.filoghost.holographicdisplays.plugin.listener.UpdateNotificationListener;
import me.filoghost.holographicdisplays.plugin.log.PrintableErrorCollector;
import me.filoghost.holographicdisplays.plugin.placeholder.registry.PlaceholderRegistry;
import me.filoghost.holographicdisplays.plugin.placeholder.tracking.ActivePlaceholderTracker;
import me.filoghost.holographicdisplays.plugin.tick.TickClock;
import me.filoghost.holographicdisplays.plugin.tick.TickingTask;
import me.filoghost.holographicdisplays.plugin.util.NMSVersion;
import me.filoghost.holographicdisplays.plugin.util.NMSVersion.OutdatedVersionException;
import me.filoghost.holographicdisplays.plugin.util.NMSVersion.UnknownVersionException;
import org.bstats.bukkit.MetricsLite;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class HolographicDisplays extends FCommonsPlugin {

    private static HolographicDisplays instance;

    private NMSManager nmsManager;
    private ConfigManager configManager;
    private BungeeServerTracker bungeeServerTracker;
    private PlaceholderRegistry placeholderRegistry;
    private LineTrackerManager lineTrackerManager;
    private InternalHologramManager internalHologramManager;
    private APIHologramManager apiHologramManager;
    private V2HologramManager v2HologramManager;
    private InternalHologramEditor internalHologramEditor;

    @Override
    public void onCheckedEnable() throws PluginEnableException {
        // Warn about plugin reloaders and the /reload command
        if (instance != null || System.getProperty("HolographicDisplaysLoaded") != null) {
            Bukkit.getConsoleSender().sendMessage(
                    ChatColor.RED + "[HolographicDisplays] Please do not use /reload or plugin reloaders."
                            + " Use the command \"/holograms reload\" instead."
                            + " You will receive no support for doing this operation.");
        }

        System.setProperty("HolographicDisplaysLoaded", "true");
        instance = this;

        if (!FeatureSupport.CHAT_COMPONENTS) {
            throw new PluginEnableException(
                    "Holographic Displays requires the new chat API.",
                    "You are probably running CraftBukkit instead of Spigot.");
        }

        if (getCommand("holograms") == null) {
            throw new PluginEnableException(
                    "Holographic Displays was unable to register the command \"holograms\".",
                    "This can be caused by edits to plugin.yml or other plugins.");
        }

        PrintableErrorCollector errorCollector = new PrintableErrorCollector();

        // Initialize class fields
        try {
            nmsManager = NMSVersion.getCurrent().createNMSManager(errorCollector);
        } catch (UnknownVersionException e) {
            throw new PluginEnableException("Holographic Displays only supports Spigot from 1.8 to 1.18.2.");
        } catch (OutdatedVersionException e) {
            throw new PluginEnableException("Holographic Displays only supports " + e.getMinimumSupportedVersion() + " and above.");
        } catch (Throwable t) {
            throw new PluginEnableException(t, "Couldn't initialize the NMS manager.");
        }

        configManager = new ConfigManager(getDataFolder().toPath());
        bungeeServerTracker = new BungeeServerTracker(this);
        placeholderRegistry = new PlaceholderRegistry();
        TickClock tickClock = new TickClock();
        ActivePlaceholderTracker placeholderTracker = new ActivePlaceholderTracker(placeholderRegistry, tickClock);
        LineClickListener lineClickListener = new LineClickListener();
        lineTrackerManager = new LineTrackerManager(nmsManager, placeholderTracker, lineClickListener, tickClock);
        apiHologramManager = new APIHologramManager(lineTrackerManager);
        v2HologramManager = new V2HologramManager(lineTrackerManager);
        Function<Position, Hologram> hologramFactory =
                (Position position) -> apiHologramManager.createHologram(position, this);
        internalHologramManager = new InternalHologramManager(hologramFactory);

        // Run only once at startup, before loading the configuration
        new SymbolsLegacyUpgrade(configManager, errorCollector).tryRun();
        new AnimationsLegacyUpgrade(configManager, errorCollector).tryRun();
        new DatabaseLegacyUpgrade(configManager, errorCollector).tryRun();

        // Load the configuration
        load(errorCollector);

        // Add packet listener for currently online players (may happen if the plugin is disabled and re-enabled)
        for (Player player : Bukkit.getOnlinePlayers()) {
            nmsManager.injectPacketListener(player, lineClickListener);
        }

        // Commands
        internalHologramEditor = new InternalHologramEditor(internalHologramManager, configManager);
        new HologramCommandManager(this, internalHologramEditor).register(this);

        // Tasks
        TickingTask tickingTask = new TickingTask(tickClock, placeholderTracker, lineTrackerManager, lineClickListener);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, tickingTask, 0, 1);

        // Listeners
        PlayerListener playerListener = new PlayerListener(nmsManager, lineClickListener, tickingTask);
        registerListener(playerListener);
        registerListener(new ChunkListener(this, apiHologramManager, v2HologramManager));
        UpdateNotificationListener updateNotificationListener = new UpdateNotificationListener();
        registerListener(updateNotificationListener);

        // Enable the APIs
        HolographicDisplaysAPIProvider.setImplementation(
                new DefaultHolographicDisplaysAPIProvider(apiHologramManager, placeholderRegistry));
        enableLegacyAPI(v2HologramManager, placeholderRegistry);

        // Setup external plugin hooks
        PlaceholderAPIHook.setup();

        // Register bStats metrics
        int pluginID = 3123;
        new MetricsLite(this, pluginID);

        // Log all loading errors at the end
        if (errorCollector.hasErrors()) {
            errorCollector.logToConsole();
            Bukkit.getScheduler().runTaskLater(this, errorCollector::logSummaryToConsole, 10L);
        }

        // Run the update checker
        updateNotificationListener.runAsyncUpdateCheck(this);
    }

    @SuppressWarnings("deprecation")
    private void enableLegacyAPI(V2HologramManager hologramManager, PlaceholderRegistry placeholderRegistry) {
        HologramsAPIProvider.setImplementation(new V2HologramsAPIProvider(hologramManager, placeholderRegistry));
    }

    public void load(ErrorCollector errorCollector) {
        internalHologramManager.deleteHolograms();

        configManager.reloadStaticReplacements(errorCollector);
        configManager.reloadMainSettings(errorCollector);

        AnimationPlaceholderFactory animationPlaceholderFactory = configManager.loadAnimations(errorCollector);
        DefaultPlaceholders.resetAndRegister(this, placeholderRegistry, animationPlaceholderFactory, bungeeServerTracker);

        bungeeServerTracker.restart(Settings.bungeeRefreshSeconds, TimeUnit.SECONDS);

        // Load holograms from database
        List<InternalHologramConfig> hologramConfigs = configManager.readHologramDatabase(errorCollector);
        for (InternalHologramConfig hologramConfig : hologramConfigs) {
            try {
                List<InternalHologramLine> lines = hologramConfig.deserializeLines();
                Position position = hologramConfig.deserializePosition();
                InternalHologram hologram = internalHologramManager.createHologram(hologramConfig.getName(), position);
                hologram.addLines(lines);
            } catch (InternalHologramLoadException e) {
                errorCollector.add(e, "error while loading hologram \"" + hologramConfig.getName() + "\"");
            }
        }

        for (BaseHologram hologram : apiHologramManager.getHolograms()) {
            hologram.getLines().updatePositions();
        }
        for (BaseHologram hologram : v2HologramManager.getHolograms()) {
            hologram.getLines().updatePositions();
        }
    }

    @Override
    public void onDisable() {
        if (lineTrackerManager != null) {
            lineTrackerManager.resetViewersAndSendDestroyPackets();
        }

        if (nmsManager != null) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                nmsManager.uninjectPacketListener(player);
            }
        }
    }

    public static HolographicDisplays getInstance() {
        return instance;
    }

    public InternalHologramEditor getInternalHologramEditor() {
        return internalHologramEditor;
    }

    public InternalHologramManager getInternalHologramManager() {
        return internalHologramManager;
    }

}
