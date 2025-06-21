package com.binaris.extracmds.command;

import net.minecraft.command.*;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemFirework;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;

public class SoarCommand extends CommandBase {

    @Override
    public String getName() {
        return "soar";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "commands.extracmds.soar.usage";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!(sender instanceof EntityPlayerMP)) {
            throw new CommandException("commands.extracmds.usage.needplayer");
        }

        if (args.length != 1) {
            throw new WrongUsageException(getUsage(sender));
        }

        int level;
        try {
            level = Integer.parseInt(args[0]);
            if (level < -128 || level > 127) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            throw new CommandException("commands.extracmds.soar.invalidlevel", args[0]);
        }

        EntityPlayerMP player = (EntityPlayerMP) sender;
        ItemStack stack = player.getHeldItemMainhand();

        if (stack.isEmpty() || !(stack.getItem() instanceof ItemFirework)) {
            throw new CommandException("commands.extracmds.usage.needitem");
        }

        NBTTagCompound fireworksTag = stack.getOrCreateSubCompound("Fireworks");
        fireworksTag.setByte("Flight", (byte) level);

        notifyCommandListener(sender, this, "commands.extracmds.soar.set", stack.getDisplayName(), level);
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }
}
