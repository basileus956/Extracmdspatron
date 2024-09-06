package com.binaris.extracmds.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Silly warnings... o-o
@SuppressWarnings("NullableProblems")
public class UnenchantCommand extends CommandBase {

    @Override
    public String getName() {
        return "unenchant";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/unenchant [enchantment]";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        ItemStack itemStack = player.getHeldItemMainhand();

        if (itemStack.isEmpty()) {
            throw new CommandException("You must be holding an item to use this command.");
        }

        NBTTagList enchantments = itemStack.getEnchantmentTagList();
        if (enchantments.hasNoTags()) {
            throw new CommandException("The item has no enchantments.");
        }

        if (args.length == 0) {
            itemStack.getTagCompound().removeTag("ench");
            notifyCommandListener(sender, this, "All enchantments have been removed.");
        } else {
            String enchantmentName = args[0];
            Enchantment enchantment = Enchantment.getEnchantmentByLocation(enchantmentName);

            if (enchantment == null) {
                throw new CommandException("Invalid enchantment: " + enchantmentName);
            }

            boolean found = false;
            for (int i = 0; i < enchantments.tagCount(); i++) {
                NBTTagCompound enchantmentCompound = enchantments.getCompoundTagAt(i);
                int enchantmentId = enchantmentCompound.getShort("id");

                if (Enchantment.getEnchantmentByID(enchantmentId) == enchantment) {
                    enchantments.removeTag(i);
                    found = true;
                    break;
                }
            }

            if (found) {
                if (enchantments.hasNoTags()) {
                    itemStack.getTagCompound().removeTag("ench");
                }
                notifyCommandListener(sender, this, "Enchantment " + enchantmentName + " has been removed.");
            } else {
                throw new CommandException("The item does not have the enchantment: " + enchantmentName);
            }
        }

        player.inventory.markDirty();
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            EntityPlayerMP player = null;
            try {
                player = getCommandSenderAsPlayer(sender);
            } catch (PlayerNotFoundException e) {
                return Collections.emptyList();
            }
            ItemStack itemStack = player.getHeldItemMainhand();
            if (!itemStack.isEmpty()) {
                NBTTagList enchantments = itemStack.getEnchantmentTagList();
                List<String> enchantmentNames = new ArrayList<>();
                for (int i = 0; i < enchantments.tagCount(); i++) {
                    NBTTagCompound enchantmentCompound = enchantments.getCompoundTagAt(i);
                    int enchantmentId = enchantmentCompound.getShort("id");
                    Enchantment enchantment = Enchantment.getEnchantmentByID(enchantmentId);
                    if (enchantment != null) {
                        enchantmentNames.add(enchantment.getRegistryName().toString());
                    }
                }
                return getListOfStringsMatchingLastWord(args, enchantmentNames);
            }
        }
        return Collections.emptyList();
    }
}
