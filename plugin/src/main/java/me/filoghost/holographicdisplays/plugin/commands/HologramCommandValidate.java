/*
 * Copyright (C) filoghost and contributors
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package me.filoghost.holographicdisplays.plugin.commands;

import me.filoghost.fcommons.command.validation.CommandException;
import me.filoghost.fcommons.command.validation.CommandValidate;
import me.filoghost.holographicdisplays.common.Utils;
import me.filoghost.holographicdisplays.plugin.disk.HologramLineParser;
import me.filoghost.holographicdisplays.plugin.disk.HologramLoadException;
import me.filoghost.holographicdisplays.plugin.hologram.internal.InternalHologram;
import me.filoghost.holographicdisplays.plugin.hologram.internal.InternalHologramLine;
import me.filoghost.holographicdisplays.plugin.hologram.internal.InternalHologramManager;
import me.filoghost.holographicdisplays.plugin.util.FileUtils;

import java.nio.file.Files;
import java.nio.file.Path;

public class HologramCommandValidate {

    public static InternalHologramLine parseHologramLine(InternalHologram hologram, String serializedLine) throws CommandException {
        try {
            return HologramLineParser.parseLine(hologram, serializedLine);
        } catch (HologramLoadException e) {
            throw new CommandException(Utils.formatExceptionMessage(e));
        }
    }

    public static InternalHologram getInternalHologram(InternalHologramManager internalHologramManager, String hologramName)
            throws CommandException {
        InternalHologram hologram = internalHologramManager.getHologramByName(hologramName);
        CommandValidate.notNull(hologram, "Cannot find a hologram named \"" + hologramName + "\".");
        return hologram;
    }

    public static Path getUserReadableFile(Path dataFolder, String fileName) throws CommandException {
        Path targetFile = dataFolder.resolve(fileName);
        CommandValidate.check(FileUtils.isInsideDirectory(targetFile, dataFolder),
                "The specified file must be inside HolographicDisplays' folder.");
        CommandValidate.check(Files.exists(targetFile),
                "The specified file \"" + fileName + "\" does not exist inside HolographicDisplays' folder.");
        CommandValidate.check(!Files.isDirectory(targetFile), "The file cannot be a folder.");
        CommandValidate.check(!isConfigFile(targetFile), "Cannot read YML configuration files.");
        return targetFile;
    }

    private static boolean isConfigFile(Path file) {
        return Files.isRegularFile(file) && file.getFileName().toString().toLowerCase().endsWith(".yml");
    }

}
