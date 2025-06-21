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

public class UnglowCommand extends CommandBase {

    @Override
    public String getName() {
        return "unglow";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "commands.extracmds.unglow.usage";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
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
            throw new CommandException("commands.extracmds.unglow.none");
        }

        boolean removedFakeEnchant = false;

        if (tag.hasKey("ench", 9)) {
            NBTTagList enchList = tag.getTagList("ench", 10);
            NBTTagList cleanedList = new NBTTagList();

            for (int i = 0; i < enchList.tagCount(); i++) {
                NBTTagCompound enchant = enchList.getCompoundTagAt(i);
                if (enchant.hasKey("id") && enchant.hasKey("lvl")) {
                    cleanedList.appendTag(enchant);
                } else {
                    removedFakeEnchant = true;
                }
            }

            if (cleanedList.tagCount() > 0) {
                tag.setTag("ench", cleanedList);
            } else {
                tag.removeTag("ench");
            }
        }

        boolean removedHideFlags = tag.hasKey("HideFlags");
        tag.removeTag("HideFlags");

        if (!removedFakeEnchant && !removedHideFlags) {
            throw new CommandException("commands.extracmds.unglow.none");
        }

        if (tag.getKeySet().isEmpty()) {
            stack.setTagCompound(null);
        }

        sender.sendMessage(new TextComponentTranslation("commands.extracmds.unglow.success"));
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("nofakeglow");
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos pos) {
        return Collections.emptyList();
    }
}
