package com.binaris.extracmds.command;

import net.minecraft.client.resources.I18n;
import net.minecraft.command.*;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.*;

@Mod.EventBusSubscriber
public class ImbueCommand extends CommandBase {

    private static final String IMBUEMENT_TAG = "Imbuement";

    @Override
    public String getName() {
        return "imbue";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "commands.extracmds.imbue.usage";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!(sender instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) sender;
        ItemStack stack = player.getHeldItemMainhand();

        if (stack == null || stack.isEmpty()) {
            throw new CommandException("commands.extracmds.usage.needitem");
        }

        if (!(stack.getItem() instanceof ItemFood)) {
            throw new CommandException("commands.extracmds.usage.needitem");
        }

        // --- BOSS-LEVEL REMOVE ---
        if (args.length >= 1 && args[0].equalsIgnoreCase("remove")) {
            if (stack.hasTagCompound() && stack.getTagCompound().hasKey(IMBUEMENT_TAG)) {
                NBTTagCompound tag = stack.getTagCompound();

                if (args.length == 1) {
                    // Remove ALL Imbuement, but keep other NBT alive
                    tag.removeTag(IMBUEMENT_TAG);
                    stack.setTagCompound(tag);
                    player.sendMessage(new TextComponentTranslation("commands.extracmds.imbue.cleared"));
                    return;
                }

                // Remove by index
                NBTTagCompound imbue = tag.getCompoundTag(IMBUEMENT_TAG);
                NBTTagList list = imbue.getTagList("Effects", 10);

                int index = parseInt(args[1], 1) - 1;
                if (index < 0 || index >= list.tagCount()) {
                    throw new CommandException("commands.extracmds.imbue.invalid_index");
                }

                list.removeTag(index);

                if (list.tagCount() == 0) {
                    tag.removeTag(IMBUEMENT_TAG);
                } else {
                    imbue.setTag("Effects", list);
                    tag.setTag(IMBUEMENT_TAG, imbue);
                }

                stack.setTagCompound(tag);
                player.sendMessage(new TextComponentTranslation("commands.extracmds.imbue.removed", index + 1));
            } else {
                throw new CommandException("commands.extracmds.imbue.empty");
            }
            return;
        }

        // --- APPLY ---
        if (args.length < 1) throw new WrongUsageException(getUsage(sender));

        Potion potion = Potion.getPotionFromResourceLocation(args[0]);
        if (potion == null) throw new CommandException("commands.extracmds.imbue.invalid_potion");

        int duration = args.length >= 2 ? parseInt(args[1]) : 30;

        int amplifier = 0;
        if (args.length >= 3) {
            amplifier = parseInt(args[2]);
            if (amplifier < 0 || amplifier > 255) {
                throw new CommandException("commands.extracmds.imbue.invalid_amplifier");
            }
        }

        boolean showParticles = args.length >= 4 ? parseBoolean(args[3]) : true;
        boolean showTooltip = args.length >= 5 ? parseBoolean(args[4]) : true;

        NBTTagCompound tag = stack.getOrCreateSubCompound(IMBUEMENT_TAG);
        NBTTagList effects = tag.getTagList("Effects", 10);

        // Remove any old duplicate
        for (int i = 0; i < effects.tagCount(); i++) {
            if (potion.getRegistryName().toString().equals(effects.getCompoundTagAt(i).getString("Id"))) {
                effects.removeTag(i);
                break;
            }
        }

        NBTTagCompound effectTag = new NBTTagCompound();
        effectTag.setString("Id", potion.getRegistryName().toString());
        effectTag.setInteger("Duration", duration);
        effectTag.setInteger("Amplifier", amplifier);
        effectTag.setBoolean("ShowParticles", showParticles);
        effectTag.setBoolean("ShowTooltip", showTooltip);

        effects.appendTag(effectTag);
        tag.setTag("Effects", effects);

        player.sendMessage(new TextComponentTranslation("commands.extracmds.imbue.applied", potion.getName()));
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) {
            List<String> base = new ArrayList<>();
            base.add("remove");
            for (ResourceLocation key : Potion.REGISTRY.getKeys()) {
                base.add(key.toString());
            }
            return getListOfStringsMatchingLastWord(args, base);
        } else if (args.length == 2 && "remove".equalsIgnoreCase(args[0])) {
            return Collections.emptyList(); // Could be upgraded to suggest indexes dynamically
        }
        return Collections.emptyList();
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return false;
    }

    @SubscribeEvent
    public static void onFoodEaten(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntityLiving() instanceof EntityPlayerMP)) return;

        ItemStack stack = event.getItem();
        if (!(stack.getItem() instanceof ItemFood)) return;

        EntityPlayerMP player = (EntityPlayerMP) event.getEntityLiving();

        if (stack.hasTagCompound() && stack.getTagCompound().hasKey(IMBUEMENT_TAG)) {
            NBTTagList list = stack.getTagCompound().getCompoundTag(IMBUEMENT_TAG).getTagList("Effects", 10);
            for (int i = 0; i < list.tagCount(); i++) {
                NBTTagCompound effectTag = list.getCompoundTagAt(i);
                Potion potion = Potion.getPotionFromResourceLocation(effectTag.getString("Id"));
                if (potion != null) {
                    PotionEffect effect = new PotionEffect(
                            potion,
                            effectTag.getInteger("Duration") * 20,
                            effectTag.getInteger("Amplifier"),
                            false,
                            effectTag.getBoolean("ShowParticles")
                    );
                    player.addPotionEffect(effect);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!stack.hasTagCompound() || !stack.getTagCompound().hasKey(IMBUEMENT_TAG)) return;

        NBTTagList list = stack.getTagCompound().getCompoundTag(IMBUEMENT_TAG).getTagList("Effects", 10);
        List<String> effects = new ArrayList<>();

        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            if (!tag.getBoolean("ShowTooltip")) continue;
            Potion potion = Potion.getPotionFromResourceLocation(tag.getString("Id"));
            if (potion != null) {
                String name = I18n.format(potion.getName());
                int duration = tag.getInteger("Duration");
                int amp = tag.getInteger("Amplifier");
                int minutes = duration / 60;
                int seconds = duration % 60;
                String time = (minutes > 0 ? minutes + ":" + String.format("%02d", seconds) : seconds + "s");
                String roman = toRoman(amp + 1);
                effects.add("§7" + name + " " + roman + " (" + time + ")");
            }
        }

        Collections.sort(effects);
        event.getToolTip().addAll(effects);
    }

    private static String toRoman(int number) {
        if (number <= 0) return "0";

        int[] values = {100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] numerals = {"C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            while (number >= values[i]) {
                number -= values[i];
                result.append(numerals[i]);
            }
        }
        return result.toString();
    }
}
