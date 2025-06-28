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
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

// Silly warnings... o-o
@SuppressWarnings("NullableProblems")
public class ReplaceLoreLineCommand extends CommandBase {
    @Override
    public String getName() {
        return "replaceloreline";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "commands.extracmds.replaceloreline.usage";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] strings) throws CommandException {
        EntityPlayerMP player;

        try {
            player = getCommandSenderAsPlayer(sender);
        } catch (PlayerNotFoundException exception) {
            if (server.sendCommandFeedback()) {
                ITextComponent textComponent = new TextComponentTranslation("commands.extracmds.usage.needplayer");
                textComponent.getStyle().setColor(TextFormatting.RED);
                sender.sendMessage(textComponent);
            }
            return;
        }

        ItemStack itemStack = player.getHeldItemMainhand();

        if (itemStack.isEmpty()) {
            if (server.sendCommandFeedback()) {
                ITextComponent textComponent = new TextComponentTranslation("commands.extracmds.usage.needitem");
                textComponent.getStyle().setColor(TextFormatting.RED);
                sender.sendMessage(textComponent);
            }
            return;
        }

        if (strings.length < 2) {
            if (server.sendCommandFeedback()) {
                ITextComponent textComponent = new TextComponentTranslation("commands.extracmds.replaceloreline.error.noLine");
                textComponent.getStyle().setColor(TextFormatting.RED);
                sender.sendMessage(textComponent);
            }
            return;
        }

        int lineNumber;
        try {
            lineNumber = Integer.parseInt(strings[0]) - 1; // Convert to zero-indexed
        } catch (NumberFormatException e) {
            ITextComponent textComponent = new TextComponentTranslation("commands.extracmds.replaceloreline.error.firstarg");
            textComponent.getStyle().setColor(TextFormatting.RED);
            sender.sendMessage(textComponent);
            return;
        }

        StringBuilder loreLine = new StringBuilder();
        for (int i = 1; i < strings.length; i++) {
            loreLine.append(strings[i]).append(" ");
        }
        loreLine.deleteCharAt(loreLine.length() - 1); // Remove last space

        loreLine = new StringBuilder(loreLine.toString().replace('&', '\u00A7'));

        NBTTagCompound displayTag = itemStack.getOrCreateSubCompound("display");
        NBTTagList loreList = displayTag.getTagList("Lore", 8);

        if (lineNumber < 0 || lineNumber >= loreList.tagCount()) {
            ITextComponent textComponent = new TextComponentTranslation("commands.extracmds.replaceloreline.error.invalidLine");
            textComponent.getStyle().setColor(TextFormatting.RED);
            sender.sendMessage(textComponent);
            return;
        }

        loreList.set(lineNumber, new NBTTagString(loreLine.toString()));
        displayTag.setTag("Lore", loreList);

        if (server.sendCommandFeedback()) {
            ITextComponent textComponent = new TextComponentTranslation(
                    "commands.extracmds.replaceloreline.success",
                    lineNumber + 1, loreLine.toString()
            );
            textComponent.getStyle().setColor(TextFormatting.GREEN);
            sender.sendMessage(textComponent);
        }
    }
}
