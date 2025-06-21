package com.binaris.extracmds.command;

import com.google.common.collect.Lists;
import net.minecraft.command.*;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FixCommand extends CommandBase {

    @Override
    public String getName() {
        return "fix";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "commands.extracmds.fix.usage";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        EntityPlayerMP player;

        try {
            player = getCommandSenderAsPlayer(sender);
        } catch (PlayerNotFoundException e) {
            if (server.sendCommandFeedback()) {
                sendError(sender, new TextComponentTranslation("commands.extracmds.usage.needplayer"));
            }
            return;
        }

        boolean isFixAll = args.length >= 1 && args[0].equalsIgnoreCase("all");

        if (isFixAll) {
            boolean repaired = fixAllItems(player);
            if (server.sendCommandFeedback()) {
                if (repaired) {
                    sendSuccess(sender, new TextComponentTranslation("commands.extracmds.fix.all"));
                } else {
                    sendError(sender, new TextComponentTranslation("commands.extracmds.fix.nothing"));
                }
            }
        } else {
            ItemStack stack = player.getHeldItemMainhand();

            if (stack.isEmpty()) {
                if (server.sendCommandFeedback()) {
                    sendError(sender, new TextComponentTranslation("commands.extracmds.usage.needitem"));
                }
                return;
            }

            boolean fixed = tryFixItem(stack);

            if (server.sendCommandFeedback()) {
                if (fixed) {
                    sendSuccess(sender, new TextComponentTranslation("commands.extracmds.fix.success"));
                } else {
                    sendError(sender, new TextComponentTranslation("commands.extracmds.fix.nodamage"));
                }
            }
        }
    }

    private static boolean fixAllItems(EntityPlayerMP player) {
        boolean repaired = false;

        for (ItemStack itemStack : player.inventory.mainInventory) {
            if (tryFixItem(itemStack)) repaired = true;
        }
        for (ItemStack itemStack : player.inventory.armorInventory) {
            if (tryFixItem(itemStack)) repaired = true;
        }
        for (ItemStack itemStack : player.inventory.offHandInventory) {
            if (tryFixItem(itemStack)) repaired = true;
        }

        return repaired;
    }

    private static boolean tryFixItem(ItemStack stack) {
        boolean fixed = false;

        // Optional: Repair Electroblob's Wizardry mana
        try {
            Class<?> manaClass = Class.forName("electroblob.wizardry.item.IManaStoringItem");

            if (manaClass.isInstance(stack.getItem())) {
                Object manaItem = stack.getItem();
                int capacity = (int) manaClass.getMethod("getManaCapacity", ItemStack.class).invoke(manaItem, stack);
                int current = (int) manaClass.getMethod("getMana", ItemStack.class).invoke(manaItem, stack);

                if (current < capacity) {
                    manaClass.getMethod("setMana", ItemStack.class, int.class).invoke(manaItem, stack, capacity);
                    fixed = true;
                }
            }
        } catch (ClassNotFoundException ignored) {
            // Wizardry not installed
        } catch (Throwable t) {
            t.printStackTrace(); // Optional: replace with logger.debug if you add a logger
        }

        // Durability repair
        if (stack.isItemDamaged() && stack.isItemStackDamageable()) {
            stack.setItemDamage(0);
            fixed = true;
        }

        return fixed;
    }

    private static void sendSuccess(ICommandSender sender, ITextComponent message) {
        message.getStyle().setColor(TextFormatting.GREEN);
        sender.sendMessage(message);
    }

    private static void sendError(ICommandSender sender, ITextComponent message) {
        message.getStyle().setColor(TextFormatting.RED);
        sender.sendMessage(message);
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        return args.length == 1 ? Lists.newArrayList("all") : Collections.emptyList();
    }
}
