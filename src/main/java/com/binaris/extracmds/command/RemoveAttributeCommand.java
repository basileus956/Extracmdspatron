package com.binaris.extracmds.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Silly warnings... o-o
@SuppressWarnings("NullableProblems")
public class RemoveAttributeCommand extends CommandBase {

    @Override
    public String getName() {
        return "removeattribute";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/removeattribute [attribute]";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        ItemStack itemStack = player.getHeldItemMainhand();

        // Check if the player is holding an item
        if (itemStack.isEmpty()) {
            throw new CommandException("You must be holding an item to use this command.");
        }

        // Check if the item has attributes
        if (!itemStack.hasTagCompound() || !itemStack.getTagCompound().hasKey("AttributeModifiers", 9)) {
            throw new CommandException("The item has no attributes.");
        }

        // Remove all attributes
        if (args.length == 0) {
            itemStack.getTagCompound().removeTag("AttributeModifiers");
            notifyCommandListener(sender, this, "All attributes have been removed.");
        } else {
            // Remove a specific attribute
            String attributeName = args[0];
            boolean found = false;

            // get the attributes tag list
            NBTTagList nbttaglist = itemStack.getTagCompound().getTagList("AttributeModifiers", 10);
            for (int i = 0; i < nbttaglist.tagCount(); i++) {
                // get the specific tag compound
                NBTTagCompound nbttagcompound = nbttaglist.getCompoundTagAt(i);
                if (nbttagcompound.getString("AttributeName").equals(attributeName)) {
                    nbttaglist.removeTag(i);
                    found = true;
                }
            }

            if (found) {
                if (nbttaglist.tagCount() == 0) {
                    itemStack.getTagCompound().removeTag("AttributeModifiers");
                }
                notifyCommandListener(sender, this, "Attribute " + attributeName + " has been removed.");
            } else {
                throw new CommandException("The item does not have the attribute: " + attributeName);
            }
        }

        player.inventory.markDirty();
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            EntityPlayerMP player;
            try {
                player = getCommandSenderAsPlayer(sender);
            } catch (PlayerNotFoundException e) {
                return Collections.emptyList();
            }
            ItemStack itemStack = player.getHeldItemMainhand();
            if (!itemStack.isEmpty() && itemStack.hasTagCompound()) {
                NBTTagList nbttaglist = itemStack.getTagCompound().getTagList("AttributeModifiers", 10);
                Set<String> attributeNames = new HashSet<>();
                for (int i = 0; i < nbttaglist.tagCount(); i++) {
                    NBTTagCompound nbttagcompound = nbttaglist.getCompoundTagAt(i);
                    attributeNames.add(nbttagcompound.getString("AttributeName"));
                }
                return getListOfStringsMatchingLastWord(args, attributeNames);
            }
        }
        return Collections.emptyList();
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return false;
    }
}


