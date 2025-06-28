package com.binaris.extracmds.command;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import net.minecraft.command.*;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

import java.util.*;

@SuppressWarnings("NullableProblems")
public class PotionCommand extends CommandBase {

    private static final Map<String, String> COLOR_NAME_MAP = new HashMap<>();
    private static final Map<String, String> COLOR_ALIAS_MAP = new HashMap<>();

    static {
        COLOR_NAME_MAP.put("black", "000000");
        COLOR_NAME_MAP.put("dark_blue", "0000AA");
        COLOR_NAME_MAP.put("dark_green", "00AA00");
        COLOR_NAME_MAP.put("dark_aqua", "00AAAA");
        COLOR_NAME_MAP.put("dark_red", "AA0000");
        COLOR_NAME_MAP.put("dark_purple", "AA00AA");
        COLOR_NAME_MAP.put("gold", "FFAA00");
        COLOR_NAME_MAP.put("grey", "AAAAAA");
        COLOR_NAME_MAP.put("dark_grey", "555555");
        COLOR_NAME_MAP.put("blue", "5555FF");
        COLOR_NAME_MAP.put("green", "55FF55");
        COLOR_NAME_MAP.put("aqua", "55FFFF");
        COLOR_NAME_MAP.put("red", "FF5555");
        COLOR_NAME_MAP.put("purple", "FF55FF");
        COLOR_NAME_MAP.put("yellow", "FFFF55");
        COLOR_NAME_MAP.put("white", "FFFFFF");
        COLOR_NAME_MAP.put("orange", "FF8800");
        COLOR_NAME_MAP.put("brown", "8B4513");
        COLOR_NAME_MAP.put("pink", "FFB6C1");

        COLOR_ALIAS_MAP.put("gray", "grey");
        COLOR_ALIAS_MAP.put("dark_gray", "dark_grey");
        COLOR_ALIAS_MAP.put("light_purple", "purple");
    }

    @Override
    public String getName() {
        return "potion";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "commands.extracmds.potion.usage";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 1 || !Lists.newArrayList("setcolour", "setcolor", "settype", "add", "remove").contains(args[0].toLowerCase())) {
            if (handleUsageError(sender, args)) return;
            sendErrorMessage(sender, "commands.extracmds.potion.usage.setcolour");
            sendErrorMessage(sender, "commands.extracmds.potion.usage.settype");
            sendErrorMessage(sender, "commands.extracmds.potion.usage.add");
            sendErrorMessage(sender, "commands.extracmds.potion.usage.remove");
            return;
        }

        if (!(sender instanceof EntityLivingBase)) {
            sendErrorMessage(sender, "commands.extracmds.potion.error.onlyliving");
            return;
        }

        EntityLivingBase entity = (EntityLivingBase) sender;
        ItemStack held = convertIfArrowOrBottle(entity, entity.getHeldItemMainhand());

        if (!isValidPotionItem(held)) {
            sendErrorMessage(sender, "commands.extracmds.usage.needitem");
            return;
        }

        switch (args[0].toLowerCase()) {
            case "setcolour":
            case "setcolor":
                handleSetColor(sender, held, args);
                break;
            case "settype":
                handleSetType(sender, held, args);
                break;
            case "add":
                handleAddEffect(sender, held, args);
                break;
            case "remove":
                handleRemove(sender, held, args);
                break;
        }
    }

    private static void handleSetColor(ICommandSender sender, ItemStack held, String[] args) {
        if (args.length < 2) {
            sendErrorMessage(sender, "commands.extracmds.potion.usage.setcolour");
            return;
        }

        String input = args[1].startsWith("#") ? args[1].substring(1) : args[1];
        String key = input.toLowerCase();
        if (COLOR_ALIAS_MAP.containsKey(key)) key = COLOR_ALIAS_MAP.get(key);
        if (COLOR_NAME_MAP.containsKey(key)) input = COLOR_NAME_MAP.get(key);

        if (!isInteger(input, 16)) {
            sendErrorMessage(sender, "commands.extracmds.potion.error.invalidcolor");
            return;
        }

        int newColor = Integer.parseInt(input, 16);
        NBTTagCompound nbt = MoreObjects.firstNonNull(held.getTagCompound(), new NBTTagCompound());
        if (nbt.hasKey("CustomPotionColor") && nbt.getInteger("CustomPotionColor") == newColor) {
            sendErrorMessage(sender, "commands.extracmds.potion.error.samecolor");
            return;
        }

        nbt.setInteger("CustomPotionColor", newColor);
        held.setTagCompound(nbt);
        sendMessage(sender, "commands.extracmds.potion.success.colour");
    }

    private static void handleSetType(ICommandSender sender, ItemStack held, String[] args) {
        if (args.length < 2) {
            sendErrorMessage(sender, "commands.extracmds.potion.usage.settype");
            return;
        }

        PotionType newType = PotionType.REGISTRY.getObject(new ResourceLocation(args[1]));
        if (newType == null) {
            sendErrorMessage(sender, "commands.extracmds.potion.error.invalidtype");
            return;
        }

        PotionType currentType = PotionUtils.getPotionFromItem(held);
        if (newType == currentType) {
            sendErrorMessage(sender, "commands.extracmds.potion.error.sametype");
            return;
        }

        PotionUtils.addPotionToItemStack(held, newType);
        sendMessage(sender, "commands.extracmds.potion.success.type");
    }

    private static void handleAddEffect(ICommandSender sender, ItemStack held, String[] args) {
        if (args.length < 2) {
            sendErrorMessage(sender, "commands.extracmds.potion.usage.add");
            return;
        }

        ResourceLocation loc = new ResourceLocation(args[1]);
        Potion potion = null;
        for (Potion pot : Iterators.toArray(Potion.REGISTRY.iterator(), Potion.class)) {
            if (pot.getRegistryName().equals(loc)) {
                potion = pot;
                break;
            }
        }

        if (potion == null) {
            sendErrorMessage(sender, "commands.extracmds.potion.error.invalideffect");
            return;
        }

        int duration = args.length >= 3 && isInteger(args[2]) ? Integer.parseInt(args[2]) * 20 : 60 * 20;
        int amplifier = args.length >= 4 && isInteger(args[3]) ? Math.min(Integer.parseInt(args[3]), Byte.MAX_VALUE) : 0;
        boolean showParticles = args.length >= 5 && Boolean.parseBoolean(args[4]);
        boolean ambient = args.length >= 6 && Boolean.parseBoolean(args[5]);

        PotionEffect newEffect = new PotionEffect(potion, duration, amplifier, ambient, showParticles);
        NBTTagCompound tag = MoreObjects.firstNonNull(held.getTagCompound(), new NBTTagCompound());
        NBTTagList list = tag.getTagList("CustomPotionEffects", 10);

        // Remove ALL matching effects to override properly
        for (int i = list.tagCount() - 1; i >= 0; i--) {
            NBTTagCompound effectNBT = list.getCompoundTagAt(i);
            PotionEffect existing = PotionEffect.readCustomPotionEffectFromNBT(effectNBT);
            if (existing.getPotion() == potion) {
                list.removeTag(i);
            }
        }

        list.appendTag(newEffect.writeCustomPotionEffectToNBT(new NBTTagCompound()));
        tag.setTag("CustomPotionEffects", list);
        held.setTagCompound(tag);
        sendMessage(sender, "commands.extracmds.potion.success.effect");
    }

    private static void handleRemove(ICommandSender sender, ItemStack held, String[] args) {
        NBTTagCompound tag = held.getTagCompound();
        if (tag == null || !tag.hasKey("CustomPotionEffects")) {
            sendErrorMessage(sender, "commands.extracmds.potion.error.invalidindex.toobig");
            return;
        }

        NBTTagList list = tag.getTagList("CustomPotionEffects", 10);

        if (args.length == 1) {
            tag.removeTag("CustomPotionEffects");
            held.setTagCompound(tag.hasNoTags() ? null : tag);
            sendMessage(sender, "commands.extracmds.potion.success.removeall");
            return;
        }

        if (!isInteger(args[1])) {
            sendErrorMessage(sender, "commands.extracmds.potion.error.invalidindex.notint");
            return;
        }

        int index = Integer.parseInt(args[1]);
        if (index <= 0 || index > list.tagCount()) {
            sendErrorMessage(sender, "commands.extracmds.potion.error.invalidindex.toobig");
            return;
        }

        PotionEffect effect = PotionEffect.readCustomPotionEffectFromNBT(list.getCompoundTagAt(index - 1));
        list.removeTag(index - 1);

        if (list.tagCount() == 0) {
            tag.removeTag("CustomPotionEffects");
            held.setTagCompound(tag.hasNoTags() ? null : tag);
        } else {
            tag.setTag("CustomPotionEffects", list);
            held.setTagCompound(tag);
        }

        sendMessage(sender, new TextComponentTranslation(
                "commands.extracmds.potion.success.remove",
                index,
                effect.getPotion().getRegistryName().toString()
        ));
    }

    private static boolean isValidPotionItem(ItemStack stack) {
        return stack.getItem() == Items.POTIONITEM ||
               stack.getItem() == Items.SPLASH_POTION ||
               stack.getItem() == Items.LINGERING_POTION ||
               stack.getItem() == Items.TIPPED_ARROW ||
               stack.getItem() == Items.GLASS_BOTTLE ||
               stack.getItem() == Items.ARROW;
    }

    private static ItemStack convertIfArrowOrBottle(EntityLivingBase entity, ItemStack held) {
        if (held.getItem() == Items.ARROW) {
            ItemStack tippedArrow = new ItemStack(Items.TIPPED_ARROW, held.getCount());
            if (held.hasTagCompound()) tippedArrow.setTagCompound(held.getTagCompound().copy());
            entity.setHeldItem(entity.getActiveHand(), tippedArrow);
            return tippedArrow;
        } else if (held.getItem() == Items.GLASS_BOTTLE) {
            ItemStack potionBottle = new ItemStack(Items.POTIONITEM, held.getCount());
            if (held.hasTagCompound()) potionBottle.setTagCompound(held.getTagCompound().copy());
            entity.setHeldItem(entity.getActiveHand(), potionBottle);
            return potionBottle;
        }
        return held;
    }

    private static boolean handleUsageError(ICommandSender sender, String[] args) {
        if (args.length < 1) return false;
        switch (args[0].toLowerCase()) {
            case "setcolour":
            case "setcolor":
                sendErrorMessage(sender, "commands.extracmds.potion.usage.setcolour");
                return true;
            case "settype":
                sendErrorMessage(sender, "commands.extracmds.potion.usage.settype");
                return true;
            case "add":
                sendErrorMessage(sender, "commands.extracmds.potion.usage.add");
                return true;
            case "remove":
                sendErrorMessage(sender, "commands.extracmds.potion.usage.remove");
                return true;
            default:
                return false;
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, Lists.newArrayList("setcolour", "settype", "add", "remove"));
        } else if (args.length == 2 && ("setcolour".equalsIgnoreCase(args[0]) || "setcolor".equalsIgnoreCase(args[0]))) {
            return getListOfStringsMatchingLastWord(args, new ArrayList<>(COLOR_NAME_MAP.keySet()));
        } else if (args.length == 2 && "add".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, Potion.REGISTRY.getKeys());
        } else if (args.length == 2 && "settype".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, PotionType.REGISTRY.getKeys());
        }
        return new ArrayList<>();
    }

    private static void sendErrorMessage(ICommandSender sender, String translationKey) {
        TextComponentTranslation text = new TextComponentTranslation(translationKey);
        text.getStyle().setColor(TextFormatting.RED);
        sender.sendMessage(text);
    }

    private static void sendMessage(ICommandSender sender, String translationKey) {
        TextComponentTranslation text = new TextComponentTranslation(translationKey);
        text.getStyle().setColor(TextFormatting.GREEN);
        sender.sendMessage(text);
    }

    private static void sendMessage(ICommandSender sender, TextComponentTranslation text) {
        text.getStyle().setColor(TextFormatting.GREEN);
        sender.sendMessage(text);
    }

    private static boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException | NullPointerException e) {
            return false;
        }
    }

    private static boolean isInteger(String s, int radix) {
        try {
            Integer.parseInt(s, radix);
            return true;
        } catch (NumberFormatException | NullPointerException e) {
            return false;
        }
    }
}