package com.binaris.extracmds.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

// Silly warnings... o-o
@SuppressWarnings("NullableProblems")
public class RenameCommand extends CommandBase {
    @Override
    public String getName() {
        return "rename";
    }

    @Override
    public String getUsage(ICommandSender iCommandSender) {
        return "commands.extracmds.rename.usage";
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

        ItemStack itemStack = player.getHeldItemMainhand();

        if(itemStack.isEmpty()){
            if(server.sendCommandFeedback()) {
                ITextComponent textComponent = new TextComponentString("You must be holding an item to use this command.");
                textComponent.getStyle().setColor(TextFormatting.RED);
                sender.sendMessage(textComponent);
                return;
            }
        }

        if(strings.length == 0){
            if(server.sendCommandFeedback()) {
                ITextComponent textComponent = new TextComponentString("You must provide a name.");
                textComponent.getStyle().setColor(TextFormatting.RED);
                sender.sendMessage(textComponent);
            }
            return;
        }

        StringBuilder name = new StringBuilder();
        for(String string : strings){
            name.append(string).append(" ");
        }
        name.deleteCharAt(name.length() - 1);

        // Replace all the & with §
        name = new StringBuilder(name.toString().replace('&', '\u00A7'));

        itemStack.setStackDisplayName(name.toString());

        if(server.sendCommandFeedback()) {
            ITextComponent textComponent = new TextComponentString("Renamed item to " + name);
            textComponent.getStyle().setColor(TextFormatting.GREEN);
            sender.sendMessage(textComponent);
        }
    }
}
