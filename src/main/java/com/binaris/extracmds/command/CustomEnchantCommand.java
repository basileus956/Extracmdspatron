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
import java.util.Collections;
import java.util.List;

// Silly warnings... o-o
@SuppressWarnings("NullableProblems")
public class CustomEnchantCommand extends CommandBase {

    @Override
    public String getName() {
        return "enchant";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/enchant [player] <enchantment> <level> || or /enchant <enchantment> <level>";
    }


    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 2) {
            throw new WrongUsageException("commands.enchant.usage", new Object[0]);
        } else {
            EntityLivingBase livingBase;
            Enchantment enchantment;
            int enchantmentLevel = 1;

            try {
                enchantment = Enchantment.getEnchantmentByID(parseInt(args[0], 0));
            } catch (NumberInvalidException var12) {
                enchantment = Enchantment.getEnchantmentByLocation(args[0]);
            }

            // Enchantment passed as first argument
            if(enchantment != null){
                livingBase = getCommandSenderAsPlayer(sender);

                enchantmentLevel = secureParseInt(args[1]);
            } else {
                livingBase = getEntity(server, sender, args[0], EntityLivingBase.class);
                try {
                    enchantment = Enchantment.getEnchantmentByID(parseInt(args[1], 0));
                } catch (NumberInvalidException var12) {
                    enchantment = Enchantment.getEnchantmentByLocation(args[1]);
                }
            }

            if (enchantment == null) {
                throw new NumberInvalidException("commands.enchant.notFound", new Object[]{args[1]});
            } else {
                ItemStack stack = livingBase.getHeldItemMainhand();
                if (stack.isEmpty()) {
                    throw new CommandException("commands.enchant.noItem", new Object[0]);
                } else {
                    if (args.length >= 3) {
                        enchantmentLevel = parseInt(args[2]);
                    }

                    setEnchantment(enchantment, enchantmentLevel, stack);
                    notifyCommandListener(sender, this, "commands.enchant.success", new Object[0]);
                    sender.setCommandStat(CommandResultStats.Type.AFFECTED_ITEMS, 1);
                }
            }
        }
    }

    private static int secureParseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private static void setEnchantment(Enchantment enchantment, int level, ItemStack stack){
        if (stack.getTagCompound() == null)
        {
            stack.setTagCompound(new NBTTagCompound());
        }

        if (!stack.getTagCompound().hasKey("ench", 9))
        {
            stack.getTagCompound().setTag("ench", new NBTTagList());
        }

        NBTTagList nbttaglist = stack.getTagCompound().getTagList("ench", 10);
        NBTTagCompound nbttagcompound = new NBTTagCompound();
        nbttagcompound.setShort("id", (short)Enchantment.getEnchantmentID(enchantment));
        nbttagcompound.setShort("lvl", (short) level);
        nbttaglist.appendTag(nbttagcompound);
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos pos) {
        if (args.length == 1) {
            List<String> list = getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
            list.addAll(getListOfStringsMatchingLastWord(args, Enchantment.REGISTRY.getKeys()));
            return list;

        } else {
            if(args.length == 2){
                if(Enchantment.getEnchantmentByLocation(args[0]) != null){
                    return Collections.emptyList();
                }
            }
            return args.length == 2 ? getListOfStringsMatchingLastWord(args, Enchantment.REGISTRY.getKeys()) : Collections.emptyList();
        }
    }

    @Override
    public boolean isUsernameIndex(String[] p_82358_1_, int p_82358_2_) {
        return p_82358_2_ == 0;
    }
}
