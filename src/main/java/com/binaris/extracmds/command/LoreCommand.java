package com.binaris.extracmds.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

// Silly warnings... o-o
@SuppressWarnings("NullableProblems")
public class LoreCommand extends CommandBase {
    @Override
    public String getName() {
        return "lore";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "commands.extracmds.lore.usage";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] strings) throws CommandException {
        EntityPlayerMP player = null;

        try {
            player = getCommandSenderAsPlayer(sender);
        } catch (PlayerNotFoundException exception) {
            if (server.sendCommandFeedback()) {
                ITextComponent textComponent = new TextComponentString("You must be a player to use this command.");
                textComponent.getStyle().setColor(TextFormatting.RED);
                sender.sendMessage(textComponent);
                return;
            }
        }

        ItemStack itemStack = player.getHeldItemMainhand();

        if (itemStack.isEmpty()) {
            if (server.sendCommandFeedback()) {
                ITextComponent textComponent = new TextComponentString("You must be holding an item to use this command.");
                textComponent.getStyle().setColor(TextFormatting.RED);
                sender.sendMessage(textComponent);
                return;
            }
        }

        if (strings.length == 0) {
            if (server.sendCommandFeedback()) {
                ITextComponent textComponent = new TextComponentString("You must provide a lore line.");
                textComponent.getStyle().setColor(TextFormatting.RED);
                sender.sendMessage(textComponent);
            }
            return;
        }

        StringBuilder loreLine = new StringBuilder();
        for (String string : strings) {
            loreLine.append(string).append(" ");
        }
        loreLine.deleteCharAt(loreLine.length() - 1);

        // Replace all the & with §
        loreLine = new StringBuilder(loreLine.toString().replace('&', '\u00A7'));

        NBTTagCompound displayTag = itemStack.getOrCreateSubCompound("display");
        NBTTagList loreList = displayTag.getTagList("Lore", 8);

        loreList.appendTag(new NBTTagString(loreLine.toString()));
        displayTag.setTag("Lore", loreList);

        if (server.sendCommandFeedback()) {
            ITextComponent textComponent = new TextComponentString("Added lore line: " + loreLine);
            textComponent.getStyle().setColor(TextFormatting.GREEN);
            sender.sendMessage(textComponent);
        }
    }
}
