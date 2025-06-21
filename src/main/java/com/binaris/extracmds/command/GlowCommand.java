package com.binaris.extracmds.command;

import net.minecraft.command.*;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class GlowCommand extends CommandBase {

    @Override
    public String getName() {
        return "glow";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "commands.extracmds.glow.usage";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2; // OP-only
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        ItemStack stack = player.getHeldItemMainhand();

        if (stack.isEmpty()) {
            throw new CommandException("commands.extracmds.usage.needitem");
        }

        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }

        boolean alreadyHasGlow = false;

        if (tag.hasKey("ench", 9)) {
            NBTTagList ench = tag.getTagList("ench", 10);
            for (int i = 0; i < ench.tagCount(); i++) {
                if (!ench.getCompoundTagAt(i).hasKey("id")) {
                    alreadyHasGlow = true;
                    break;
                }
            }
        }

        if (alreadyHasGlow) {
            throw new CommandException("commands.extracmds.glow.extant");
        }

        if (!tag.hasKey("ench", 9)) {
            NBTTagList enchList = new NBTTagList();
            NBTTagCompound emptyEnchant = new NBTTagCompound();
            enchList.appendTag(emptyEnchant);
            tag.setTag("ench", enchList);
        }

        tag.setInteger("HideFlags", 1);
        sender.sendMessage(new TextComponentTranslation("commands.extracmds.glow.success"));
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("fakeglow");
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos pos) {
        return Collections.emptyList();
    }
}
