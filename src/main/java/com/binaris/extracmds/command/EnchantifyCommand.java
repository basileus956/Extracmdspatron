package com.binaris.extracmds.command;

import net.minecraft.command.*;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EntityLivingBase;
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
public class EnchantifyCommand extends CommandBase {

    @Override
    public String getName() {
        return "enchantify";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "commands.extracmds.enchantify.usage";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 1) {
            throw new WrongUsageException(getUsage(sender));
        }

        Enchantment enchantment;
        int enchantmentLevel = 1;

        try {
            enchantment = Enchantment.getEnchantmentByID(parseInt(args[0], 0));
        } catch (NumberInvalidException e) {
            enchantment = Enchantment.getEnchantmentByLocation(args[0]);
        }

        if (enchantment == null) {
            throw new NumberInvalidException("commands.extracmds.enchantify.notFound", args[0]);
        }

        EntityLivingBase livingBase = getCommandSenderAsPlayer(sender);
        ItemStack stack = livingBase.getHeldItemMainhand();

        if (stack.isEmpty()) {
            throw new CommandException("commands.extracmds.enchantify.noItem");
        }

        if (args.length >= 2) {
            try {
                enchantmentLevel = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                throw new CommandException("commands.extracmds.enchantify.invalidLevel");
            }
        }

        setEnchantment(enchantment, enchantmentLevel, stack);
        notifyCommandListener(sender, this, "commands.enchant.success");
        sender.setCommandStat(CommandResultStats.Type.AFFECTED_ITEMS, 1);
    }

    private static void setEnchantment(Enchantment enchantment, int level, ItemStack stack) {
        if (stack.getTagCompound() == null) {
            stack.setTagCompound(new NBTTagCompound());
        }

        NBTTagCompound tag = stack.getTagCompound();
        NBTTagList enchantList = tag.hasKey("ench", 9) ? tag.getTagList("ench", 10) : new NBTTagList();
        boolean updated = false;

        for (int i = 0; i < enchantList.tagCount(); i++) {
            NBTTagCompound enchantData = enchantList.getCompoundTagAt(i);
            if (enchantData.getShort("id") == Enchantment.getEnchantmentID(enchantment)) {
                enchantData.setShort("lvl", (short) level);
                updated = true;
                break;
            }
        }

        if (!updated) {
            NBTTagCompound enchantData = new NBTTagCompound();
            enchantData.setShort("id", (short) Enchantment.getEnchantmentID(enchantment));
            enchantData.setShort("lvl", (short) level);
            enchantList.appendTag(enchantData);
        }

        tag.setTag("ench", enchantList);
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos pos) {
        if (args.length == 1) {
            return new ArrayList<>(getListOfStringsMatchingLastWord(args, Enchantment.REGISTRY.getKeys()));
        }
        return Collections.emptyList();
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return false;
    }
}