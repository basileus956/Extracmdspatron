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
        return "/removeattribute [attribute] [slot]";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        ItemStack itemStack = player.getHeldItemMainhand();

        if (itemStack.isEmpty()) {
            throw new CommandException("You must be holding an item to use this command.");
        }

        if (!itemStack.hasTagCompound() || !itemStack.getTagCompound().hasKey("AttributeModifiers", 9)) {
            throw new CommandException("The item has no attributes.");
        }

        NBTTagList nbttaglist = itemStack.getTagCompound().getTagList("AttributeModifiers", 10);

        if (args.length == 0) {
            itemStack.getTagCompound().removeTag("AttributeModifiers");
            notifyCommandListener(sender, this, "All attributes have been removed.");
        } else {
            String attributeName = args[0];
            String targetSlot = args.length >= 2 ? args[1] : null;
            boolean found = false;

            for (int i = 0; i < nbttaglist.tagCount(); i++) {
                NBTTagCompound nbttagcompound = nbttaglist.getCompoundTagAt(i);
                boolean nameMatches = nbttagcompound.getString("AttributeName").equals(attributeName);
                boolean slotMatches = (targetSlot == null || nbttagcompound.getString("Slot").equalsIgnoreCase(targetSlot));

                if (nameMatches && slotMatches) {
                    nbttaglist.removeTag(i);
                    found = true;
                    i--; // Adjust index after removal
                    if (targetSlot != null) break; // Remove only one match if slot specified
                }
            }

            if (found) {
                if (nbttaglist.tagCount() == 0) {
                    itemStack.getTagCompound().removeTag("AttributeModifiers");
                }
                String msg = "Attribute " + attributeName + " has been removed";
                if (targetSlot != null) msg += " from the " + targetSlot + " slot";
                notifyCommandListener(sender, this, msg + ".");
            } else {
                throw new CommandException("The item does not have the attribute: " + attributeName +
                        (targetSlot != null ? " on slot: " + targetSlot : ""));
            }
        }

        player.inventory.markDirty();
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        try {
            EntityPlayerMP player = getCommandSenderAsPlayer(sender);
            ItemStack itemStack = player.getHeldItemMainhand();

            if (itemStack.isEmpty() || !itemStack.hasTagCompound()) return Collections.emptyList();

            NBTTagList nbttaglist = itemStack.getTagCompound().getTagList("AttributeModifiers", 10);

            if (args.length == 1) {
                Set<String> attributeNames = new HashSet<>();
                for (int i = 0; i < nbttaglist.tagCount(); i++) {
                    NBTTagCompound nbttagcompound = nbttaglist.getCompoundTagAt(i);
                    attributeNames.add(nbttagcompound.getString("AttributeName"));
                }
                return getListOfStringsMatchingLastWord(args, attributeNames);
            } else if (args.length == 2) {
                String selectedAttribute = args[0];
                Set<String> matchingSlots = new HashSet<>();
                for (int i = 0; i < nbttaglist.tagCount(); i++) {
                    NBTTagCompound nbttagcompound = nbttaglist.getCompoundTagAt(i);
                    if (nbttagcompound.getString("AttributeName").equals(selectedAttribute)) {
                        matchingSlots.add(nbttagcompound.getString("Slot"));
                    }
                }
                return getListOfStringsMatchingLastWord(args, matchingSlots);
            }
        } catch (PlayerNotFoundException ignored) {}

        return Collections.emptyList();
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return false;
    }
}

