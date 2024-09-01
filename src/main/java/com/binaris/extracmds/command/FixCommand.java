package com.binaris.extracmds.command;

import com.binaris.extracmds.ExtraCMDS;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

// Silly warnings... o-o
@SuppressWarnings("NullableProblems")
public class FixCommand extends CommandBase {
    @Override
    public String getName() {
        return "fix";
    }

    @Override
    public String getUsage(ICommandSender iCommandSender) {
        return "commands." + ExtraCMDS.MODID + ".fix.usage";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 4;
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
}
