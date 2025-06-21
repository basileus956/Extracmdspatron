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
import net.minecraft.util.text.translation.I18n;

import javax.annotation.Nullable;
import java.util.*;

@SuppressWarnings("NullableProblems")
public class RemoveAttributeCommand extends CommandBase {

    @Override
    public String getName() {
        return "removeattribute";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "commands.extracmds.removeattribute.usage";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        ItemStack itemStack = player.getHeldItemMainhand();

        if (itemStack.isEmpty()) {
            throw new CommandException(I18n.translateToLocal("commands.extracmds.removeattribute.need_item"));
        }

        if (!itemStack.hasTagCompound() || !itemStack.getTagCompound().hasKey("AttributeModifiers", 9)) {
            throw new CommandException(I18n.translateToLocal("commands.extracmds.removeattribute.no_attributes"));
        }

        NBTTagCompound tag = itemStack.getTagCompound();
        NBTTagList nbttaglist = tag.getTagList("AttributeModifiers", 10);

        if (args.length == 0) {
            tag.removeTag("AttributeModifiers");
            if (tag.hasNoTags()) {
                itemStack.setTagCompound(null);
            }
            notifyCommandListener(sender, this, I18n.translateToLocal("commands.extracmds.removeattribute.all_removed"));
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
                    i--;
                    if (targetSlot != null) break;
                }
            }

            if (found) {
                if (nbttaglist.tagCount() == 0) {
                    tag.removeTag("AttributeModifiers");
                    if (tag.hasNoTags()) {
                        itemStack.setTagCompound(null);
                    }
                }
                if (targetSlot != null) {
                    notifyCommandListener(sender, this,
                            I18n.translateToLocalFormatted("commands.extracmds.removeattribute.removed_slot", attributeName, targetSlot));
                } else {
                    notifyCommandListener(sender, this,
                            I18n.translateToLocalFormatted("commands.extracmds.removeattribute.removed", attributeName));
                }
            } else {
                throw new CommandException(
                        I18n.translateToLocalFormatted("commands.extracmds.removeattribute.not_found",
                                attributeName, targetSlot != null ? targetSlot : "-")
                );
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
