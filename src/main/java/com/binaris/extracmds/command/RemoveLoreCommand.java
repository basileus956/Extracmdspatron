package com.binaris.extracmds.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;

// Silly warnings... o-o
@SuppressWarnings("NullableProblems")
public class RemoveLoreCommand extends CommandBase {

    @Override
    public String getName() {
        return "removelore";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "commands.extracmds.removelore.usage";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!(sender instanceof EntityPlayerMP)) {
            throw new CommandException("commands.extracmds.usage.needplayer");
        }

        EntityPlayerMP player = (EntityPlayerMP) sender;
        ItemStack itemStack = player.getHeldItemMainhand();

        if (itemStack.isEmpty()) {
            throw new CommandException("commands.extracmds.usage.needitem");
        }

        NBTTagCompound nbt = itemStack.getTagCompound();

        if (nbt == null || !nbt.hasKey("display", 10)) {
            throw new CommandException("commands.extracmds.removelore.error.nolore");
        }

        NBTTagCompound display = nbt.getCompoundTag("display");

        if (!display.hasKey("Lore", 9)) {
            throw new CommandException("commands.extracmds.removelore.error.nolore");
        }

        NBTTagList loreList = display.getTagList("Lore", 8);

        // If no argument provided, remove all lore
        if (args.length == 0) {
            display.removeTag("Lore");
        } else {
            // Parse the line number
            int line;
            try {
                line = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                throw new CommandException("commands.extracmds.removelore.error.invalid");
            }

            // Validate the line number
            if (line < 1 || line > loreList.tagCount()) {
                throw new CommandException("commands.extracmds.removelore.bounds");
            }

            // Remove the specific line (0-indexed in NBTTagList)
            loreList.removeTag(line - 1);

            // If the lore list is now empty, remove the "Lore" tag entirely
            if (loreList.tagCount() == 0) {
                display.removeTag("Lore");
            }
        }

        // Save changes back to the item
        if (display.hasNoTags()) {
            nbt.removeTag("display");
        }

        if (nbt.hasNoTags()) {
            itemStack.setTagCompound(null);
        }

        // Provide feedback to the player
        if (args.length == 0) {
            player.sendMessage(new TextComponentTranslation("commands.extracmds.removelore.success.all"));
        } else {
            player.sendMessage(new TextComponentTranslation("commands.extracmds.removelore.success", args[0]));
        }
    }
}

