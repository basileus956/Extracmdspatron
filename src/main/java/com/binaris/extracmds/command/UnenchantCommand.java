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

@SuppressWarnings("NullableProblems")
public class UnenchantCommand extends CommandBase {

    @Override
    public String getName() {
        return "unenchant";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "commands.extracmds.unenchant.usage";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        ItemStack itemStack = player.getHeldItemMainhand();

        if (itemStack.isEmpty()) {
            throw new CommandException("commands.extracmds.usage.needitem");
        }

        NBTTagList enchantments = itemStack.getEnchantmentTagList();
        if (enchantments.hasNoTags()) {
            throw new CommandException("command.extracmds.usage.needenchant");
        }

        if (args.length == 0) {
            NBTTagCompound tag = itemStack.getTagCompound();
            if (tag != null) {
                tag.removeTag("ench");
                if (tag.hasNoTags()) {
                    itemStack.setTagCompound(null);
                }
            }
            notifyCommandListener(sender, this, "commands.extracmds.unenchant.remove");
        } else {
            String enchantmentName = args[0];
            Enchantment enchantment = Enchantment.getEnchantmentByLocation(enchantmentName);

            if (enchantment == null) {
                throw new CommandException("commands.extracmds.unenchant.invalid", enchantmentName);
            }

            boolean found = false;

            for (int i = enchantments.tagCount() - 1; i >= 0; i--) {
                NBTTagCompound enchantmentCompound = enchantments.getCompoundTagAt(i);
                int enchantmentId = enchantmentCompound.getShort("id");

                if (Enchantment.getEnchantmentByID(enchantmentId) == enchantment) {
                    enchantments.removeTag(i);
                    found = true;
                }
            }

            if (!found) {
                throw new CommandException("commands.extracmds.unenchant.noenchant", enchantmentName);
            }

            if (enchantments.hasNoTags()) {
                NBTTagCompound tag = itemStack.getTagCompound();
                if (tag != null) {
                    tag.removeTag("ench");
                    if (tag.hasNoTags()) {
                        itemStack.setTagCompound(null);
                    }
                }
            }

            notifyCommandListener(sender, this, "commands.extracmds.unenchant.success", enchantmentName);
        }

        player.inventory.markDirty();
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            try {
                EntityPlayerMP player = getCommandSenderAsPlayer(sender);
                ItemStack itemStack = player.getHeldItemMainhand();

                if (!itemStack.isEmpty()) {
                    NBTTagList enchantments = itemStack.getEnchantmentTagList();
                    List<String> enchantmentNames = new ArrayList<>();

                    for (int i = 0; i < enchantments.tagCount(); i++) {
                        NBTTagCompound enchantmentCompound = enchantments.getCompoundTagAt(i);
                        int enchantmentId = enchantmentCompound.getShort("id");
                        Enchantment enchantment = Enchantment.getEnchantmentByID(enchantmentId);

                        if (enchantment != null && enchantment.getRegistryName() != null) {
                            enchantmentNames.add(enchantment.getRegistryName().toString());
                        }
                    }

                    String userInput = args[0].toLowerCase();
                    List<String> matches = new ArrayList<>();

                    for (String fullName : enchantmentNames) {
                        String[] parts = fullName.split(":");
                        String path = parts.length > 1 ? parts[1] : parts[0];

                        if (fullName.toLowerCase().startsWith(userInput) || path.toLowerCase().startsWith(userInput)) {
                            matches.add(fullName);
                        }
                    }

                    return matches;
                }
            } catch (PlayerNotFoundException ignored) {
                return Collections.emptyList();
            }
        }

        return Collections.emptyList();
    }
}
