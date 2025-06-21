package com.binaris.extracmds.command;

import net.minecraft.command.*;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class UnbreakableCommand extends CommandBase {

    @Override
    public String getName() {
        return "unbreakable";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "commands.extracmds.unbreakable.usage";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length != 1 || (!"true".equalsIgnoreCase(args[0]) && !"false".equalsIgnoreCase(args[0]))) {
            throw new WrongUsageException(getUsage(sender));
        }

        if (!(sender instanceof EntityPlayerMP)) {
            throw new WrongUsageException("commands.extracmds.usage.needplayer");
        }

        EntityPlayerMP player = (EntityPlayerMP) sender;
        ItemStack heldItem = player.getHeldItemMainhand();

        if (heldItem.isEmpty()) {
            throw new CommandException("commands.extracmds.usage.needitem");
        }

        boolean unbreakable = Boolean.parseBoolean(args[0]);
        NBTTagCompound tag = heldItem.getTagCompound();

        if (tag == null) {
            if (!unbreakable) {
                throw new CommandException("commands.extracmds.unbreakable.already", unbreakable);
            }
            tag = new NBTTagCompound();
            heldItem.setTagCompound(tag);
        }

        boolean current = tag.getBoolean("Unbreakable");
        if (current == unbreakable) {
            throw new CommandException("commands.extracmds.unbreakable.already", unbreakable);
        }

        if (unbreakable) {
            tag.setBoolean("Unbreakable", true);
        } else {
            tag.removeTag("Unbreakable");

            // If removing this tag leaves the NBT compound empty, leave it alone — no need to clear entire tag compound.
        }

        notifyCommandListener(sender, this, "commands.extracmds.unbreakable.set", unbreakable);
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return sender.canUseCommand(2, getName());
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos pos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "true", "false");
        }
        return Collections.emptyList();
    }
}
