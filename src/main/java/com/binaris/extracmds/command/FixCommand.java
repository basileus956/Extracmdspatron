package com.binaris.extracmds.command;

import com.google.common.collect.Lists;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

// Silly warnings... o-o
@SuppressWarnings("NullableProblems")
public class FixCommand extends CommandBase {
    @Override
    public String getName() {
        return "fix";
    }

    @Override
    public String getUsage(ICommandSender iCommandSender) {
        return "/fix or /fix all";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender iCommandSender, String[] strings) throws CommandException {
        EntityPlayerMP player = null;

        try{
            player = getCommandSenderAsPlayer(iCommandSender);
        }catch (PlayerNotFoundException exception){
            if(server.sendCommandFeedback()) {
                ITextComponent textComponent = new TextComponentString("You must be a player to use this command.");
                textComponent.getStyle().setColor(TextFormatting.RED);
                iCommandSender.sendMessage(textComponent);
                return;
            }
        }

        // /fix all
        if(strings.length >= 1){
            if(strings[0].equalsIgnoreCase("all")){
                boolean repairSomething = false;

                for (ItemStack itemStack : player.inventory.mainInventory) {
                    if(itemStack.isItemDamaged() && itemStack.isItemStackDamageable()){
                        itemStack.setItemDamage(0);
                        repairSomething = true;
                    }
                }

                for (ItemStack itemStack : player.inventory.armorInventory) {
                    if(itemStack.isItemDamaged() && itemStack.isItemStackDamageable()){
                        itemStack.setItemDamage(0);
                        repairSomething = true;
                    }
                }

                for (ItemStack itemStack : player.inventory.offHandInventory) {
                    if(itemStack.isItemDamaged() && itemStack.isItemStackDamageable()){
                        itemStack.setItemDamage(0);
                        repairSomething = true;
                    }
                }

                if(server.sendCommandFeedback()) {
                    if(!repairSomething){
                        ITextComponent textComponent = new TextComponentString("No items to repair.");
                        textComponent.getStyle().setColor(TextFormatting.RED);
                        iCommandSender.sendMessage(textComponent);
                        return;
                    }

                    ITextComponent textComponent = new TextComponentString("All items fixed.");
                    textComponent.getStyle().setColor(TextFormatting.GREEN);
                    iCommandSender.sendMessage(textComponent);
                }
                return;
            }
        }

        // /fix normal command
        ItemStack itemStack = player.getHeldItemMainhand();

        if(itemStack.isEmpty()){
            if(server.sendCommandFeedback()) {
                ITextComponent textComponent = new TextComponentString("You must be holding an item to use this command.");
                textComponent.getStyle().setColor(TextFormatting.RED);
                iCommandSender.sendMessage(textComponent);
                return;
            }
        } else {
            if(!itemStack.isItemDamaged() || !itemStack.isItemStackDamageable()){
                if(server.sendCommandFeedback()) {
                    ITextComponent textComponent = new TextComponentString("Item is not damaged.");
                    textComponent.getStyle().setColor(TextFormatting.RED);
                    iCommandSender.sendMessage(textComponent);
                }
                return;
            }

            itemStack.setItemDamage(0);
            if(server.sendCommandFeedback()) {
                ITextComponent textComponent = new TextComponentString("Item fixed.");
                textComponent.getStyle().setColor(TextFormatting.GREEN);
                iCommandSender.sendMessage(textComponent);
            }
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        List<String> list = Lists.newArrayList("all");
        return args.length == 1 ? list : Collections.emptyList();
    }
}
