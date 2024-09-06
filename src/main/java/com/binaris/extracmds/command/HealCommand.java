package com.binaris.extracmds.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

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
        return "/heal or /heal <player>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        EntityPlayerMP player;

        // If no arguments, heal the command sender if they are a player
        if (args.length == 0) {
            if (sender instanceof EntityPlayerMP) {
                player = (EntityPlayerMP) sender;
            } else {
                throw new CommandException("You must specify a player to heal if not a player.");
            }
        } else {
            // Find the player specified in the arguments
            player = getPlayer(server, sender, args[0]);
        }

        // Heal the player to full health
        player.setHealth(player.getMaxHealth());

        // Send feedback to the player and the command sender (if different)
        if (sender.equals(player)) {
            sender.sendMessage(new TextComponentString("You have been healed to full health."));
        } else {
            sender.sendMessage(new TextComponentString("Healed " + player.getName() + " to full health."));
            player.sendMessage(new TextComponentString("You have been healed to full health."));
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

