package com.binaris.extracmds.command;

import com.binaris.extracmds.ExtraCMDS;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

// Silly warnings... o-o
@SuppressWarnings("NullableProblems")
public class SkullCommand extends CommandBase {
    @Override
    public String getName() {
        return "skull";
    }

    @Override
    public String getUsage(ICommandSender iCommandSender) {
        return "/skull <player> or /skull";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] strings) throws CommandException {
        EntityPlayerMP player = null;

        try{
            player = getCommandSenderAsPlayer(sender);
        }catch (PlayerNotFoundException exception){
            if(server.sendCommandFeedback()) {
                ITextComponent textComponent = new TextComponentString("You must be a player to use this command.");
                textComponent.getStyle().setColor(TextFormatting.RED);
                sender.sendMessage(textComponent);
                return;
            }
        }

        String playerHeadName;
        if(strings.length == 0){
            playerHeadName = player.getName();
        } else {
            playerHeadName = strings[0];
        }

        if(strings.length > 1){
            if(server.sendCommandFeedback()) {
                ITextComponent textComponent = new TextComponentString("Too many arguments.");
                textComponent.getStyle().setColor(TextFormatting.RED);
                sender.sendMessage(textComponent);
            }
            return;
        }

        server.commandManager.executeCommand(sender, "give " + player.getName() + " minecraft:skull 1 3 {SkullOwner:" + playerHeadName + "}");
    }
}
