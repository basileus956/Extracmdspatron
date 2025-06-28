package com.binaris.extracmds.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;

import javax.annotation.Nullable;
import java.util.List;

// Silly warnings... o-o
@SuppressWarnings("NullableProblems")
public class HealCommand extends CommandBase {

    @Override
    public String getName() {
        return "heal";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "commands.extracmds.heal.usage";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        EntityPlayerMP player;

        // If no arguments, heal the command sender if they are a player
        if (args.length == 0) {
            if (sender instanceof EntityPlayerMP) {
                player = (EntityPlayerMP) sender;
            } else {
                throw new CommandException("commands.extracmds.heal.error.not_player");
            }
        } else {
            // Find the player specified in the arguments
            player = getPlayer(server, sender, args[0]);
        }

        // Heal the player to full health
        player.setHealth(player.getMaxHealth());

        // Send feedback to the player and the command sender (if different)
        if (sender.equals(player)) {
            sender.sendMessage(new TextComponentTranslation("commands.extracmds.heal.success.self"));
        } else {
            sender.sendMessage(new TextComponentTranslation("commands.extracmds.heal.success.other", player.getName()));
            player.sendMessage(new TextComponentTranslation("commands.extracmds.heal.success.self"));
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
        } else {
            return super.getTabCompletions(server, sender, args, targetPos);
        }
    }
}

