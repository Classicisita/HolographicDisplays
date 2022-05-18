/*
 * Copyright (C) filoghost and contributors
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package me.filoghost.holographicdisplays.plugin.commands.subs;

import me.filoghost.fcommons.command.sub.SubCommandContext;
import me.filoghost.fcommons.command.validation.CommandException;
import me.filoghost.fcommons.command.validation.CommandValidate;
import me.filoghost.holographicdisplays.api.beta.Position;
import me.filoghost.holographicdisplays.plugin.commands.HologramSubCommand;
import me.filoghost.holographicdisplays.plugin.commands.InternalHologramEditor;
import me.filoghost.holographicdisplays.plugin.event.InternalHologramChangeEvent.ChangeType;
import me.filoghost.holographicdisplays.plugin.format.ColorScheme;
import me.filoghost.holographicdisplays.plugin.internal.hologram.InternalHologram;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MoveHereCommand extends HologramSubCommand {

    private final InternalHologramEditor hologramEditor;

    public MoveHereCommand(InternalHologramEditor hologramEditor) {
        super("moveHere");
        setMinArgs(1);
        setUsageArgs("<hologram>");
        setDescription("Moves a hologram to your position.");

        this.hologramEditor = hologramEditor;
    }

    @Override
    public void execute(CommandSender sender, String[] args, SubCommandContext context) throws CommandException {
        Player player = CommandValidate.getPlayerSender(sender);
        InternalHologram hologram = hologramEditor.getExistingHologram(args[0]);

        hologram.setPosition(Position.of(player.getLocation()));
        hologramEditor.saveChanges(hologram, ChangeType.EDIT_POSITION);

        hologramEditor.teleportLookingDown(player, player.getLocation());
        player.sendMessage(ColorScheme.PRIMARY + "Hologram \"" + hologram.getName() + "\" moved to your position.");
    }

}
