/*
 * Copyright (C) filoghost and contributors
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package me.filoghost.holographicdisplays.plugin.commands.subs;

import me.filoghost.fcommons.Strings;
import me.filoghost.fcommons.command.sub.SubCommandContext;
import me.filoghost.fcommons.command.validation.CommandException;
import me.filoghost.fcommons.command.validation.CommandValidate;
import me.filoghost.holographicdisplays.api.beta.Position;
import me.filoghost.holographicdisplays.plugin.commands.HologramSubCommand;
import me.filoghost.holographicdisplays.plugin.commands.InternalHologramEditor;
import me.filoghost.holographicdisplays.plugin.event.InternalHologramChangeEvent.ChangeType;
import me.filoghost.holographicdisplays.plugin.format.ColorScheme;
import me.filoghost.holographicdisplays.plugin.internal.hologram.InternalHologram;
import me.filoghost.holographicdisplays.plugin.internal.hologram.InternalHologramLine;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CreateCommand extends HologramSubCommand {

    private final InternalHologramEditor hologramEditor;

    public CreateCommand(InternalHologramEditor hologramEditor) {
        super("create");
        setMinArgs(1);
        setUsageArgs("<hologramName> [text]");
        setDescription(
                "Creates a new hologram with the given name, that must",
                "be alphanumeric. The name will be used as reference to",
                "that hologram for editing commands.");

        this.hologramEditor = hologramEditor;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void execute(CommandSender sender, String[] args, SubCommandContext context) throws CommandException {
        Player player = CommandValidate.getPlayerSender(sender);
        String hologramName = args[0];

        CommandValidate.check(hologramName.matches("[a-zA-Z0-9_\\-]+"),
                "The name must contain only alphanumeric characters, underscores and hyphens.");
        CommandValidate.check(!hologramEditor.hologramExists(hologramName), "A hologram with that name already exists.");

        Position spawnPosition = Position.of(player.getLocation());
        boolean moveUp = player.isOnGround();

        if (moveUp) {
            spawnPosition = spawnPosition.add(0, 1.2, 0);
        }

        InternalHologram hologram = hologramEditor.create(hologramName, spawnPosition);
        InternalHologramLine line;

        if (args.length > 1) {
            String text = Strings.joinFrom(" ", args, 1);
            line = hologramEditor.parseHologramLine(text);
            player.sendMessage(ColorScheme.SECONDARY_DARK + "(Change the lines with /" + context.getRootLabel()
                    + " edit " + hologram.getName() + ")");
        } else {
            line = hologramEditor.parseHologramLine("Default hologram. Change it with "
                    + ColorScheme.PRIMARY.toString().replace(ChatColor.COLOR_CHAR, '&')
                    + "/" + context.getRootLabel() + " edit " + hologram.getName());
        }

        hologram.addLine(line);
        hologramEditor.saveChanges(hologram, ChangeType.CREATE);

        hologramEditor.teleportLookingDown(player, player.getLocation());
        player.sendMessage(ColorScheme.PRIMARY + "Hologram named \"" + hologram.getName() + "\" created.");

        if (moveUp) {
            player.sendMessage(ColorScheme.SECONDARY_DARK + "(You were on the ground,"
                    + " the hologram was automatically moved up."
                    + " If you use /" + context.getRootLabel() + " moveHere " + hologram.getName() + ","
                    + " the hologram will be moved to your feet)");
        }
    }

}
