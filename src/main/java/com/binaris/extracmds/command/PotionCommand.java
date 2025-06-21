package com.binaris.extracmds.command;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionType;
import net.minecraft.potion.PotionUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// This command is based in the original /potion command from the mod "MoreCommands",
// I just refactored it to be better to read and understand and added some new features.

// Silly warnings... o-o
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
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 2 || !Lists.newArrayList("setcolour", "setcolor", "settype", "add", "remove").contains(args[0].toLowerCase())) {
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
        ItemStack held = entity.getHeldItemMainhand();

        switch (args[0].toLowerCase()) {
            case "setcolour":
            case "setcolor": {
                String input = args[1].startsWith("#") ? args[1].substring(1) : args[1];
                String key = input.toLowerCase();
                if (COLOR_ALIAS_MAP.containsKey(key)) key = COLOR_ALIAS_MAP.get(key);
                if (COLOR_NAME_MAP.containsKey(key)) input = COLOR_NAME_MAP.get(key);
                if (!isInteger(input, 16)) {
                    sendErrorMessage(sender, "commands.extracmds.potion.error.invalidcolor");
                    return;
                }
                held = convertIfArrowOrBottle(entity, held);
                NBTTagCompound nbt = MoreObjects.firstNonNull(held.getTagCompound(), new NBTTagCompound());
                nbt.setInteger("CustomPotionColor", Integer.parseInt(input, 16));
                held.setTagCompound(nbt);
                sendMessage(sender, "commands.extracmds.potion.success.colour");
                break;
            }
            case "settype": {
                PotionType type = PotionType.REGISTRY.getObject(new ResourceLocation(args[1]));
                if (type == null) {
                    sendErrorMessage(sender, "commands.extracmds.potion.error.invalidtype");
                    return;
                }
                held = convertIfArrowOrBottle(entity, held);
                PotionUtils.addPotionToItemStack(held, type);
                sendMessage(sender, "commands.extracmds.potion.success.type");
                break;
            }
            case "add": {
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
                held = convertIfArrowOrBottle(entity, held);
                int duration = args.length >= 3 && isInteger(args[2]) ? Integer.parseInt(args[2]) * 20 : 60 * 20;
                int amplifier = args.length >= 4 && isInteger(args[3]) ? Math.min(Integer.parseInt(args[3]), Byte.MAX_VALUE) : 0;
                boolean showParticles = args.length >= 5 && Boolean.parseBoolean(args[4]);
                boolean ambient = args.length >= 6 && Boolean.parseBoolean(args[5]);
                NBTTagList oldTagList = MoreObjects.firstNonNull(held.getTagCompound(), new NBTTagCompound()).getTagList("CustomPotionEffects", 10).copy();
                PotionUtils.appendEffects(held, Lists.newArrayList(new PotionEffect(potion, duration, amplifier, ambient, showParticles)));
                NBTTagList newTagList = MoreObjects.firstNonNull(held.getTagCompound(), new NBTTagCompound()).getTagList("CustomPotionEffects", 10);
                oldTagList.forEach(nbt -> newTagList.appendTag(nbt));
                sendMessage(sender, "commands.extracmds.potion.success.effect");
                break;
            }
            case "remove": {
                NBTTagList list = MoreObjects.firstNonNull(held.getTagCompound(), new NBTTagCompound()).getTagList("CustomPotionEffects", 10);
                if (!isInteger(args[1])) {
                    sendErrorMessage(sender, "commands.extracmds.potion.error.invalidindex.notint");
                    return;
                }
                int index = Integer.parseInt(args[1]);
                if (index <= 0) {
                    sendErrorMessage(sender, "commands.extracmds.potion.error.invalidindex.zero");
                    return;
                }
                if (index > list.tagCount()) {
                    sendErrorMessage(sender, "commands.extracmds.potion.error.invalidindex.toobig");
                    return;
                }
                held = convertIfArrowOrBottle(entity, held);
                PotionEffect effect = PotionEffect.readCustomPotionEffectFromNBT((NBTTagCompound) list.get(index - 1));
                list.removeTag(index - 1);
                sendMessage(sender, "commands.extracmds.potion.success.remove" ,index, effect.getPotion().getRegistryName(), effect.getDuration() / 20, effect.getAmplifier(), effect.getIsAmbient());
                break;
            }
        }
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
                sendErrorMessage(sender, "Usage: /potion <setcolour/setcolor> <colour>");
                return true;
            case "settype":
                sendErrorMessage(sender, "Usage: /potion <settype> <type>");
                return true;
            case "add":
                sendErrorMessage(sender, "Usage: /potion <add> <effect> [duration] [amplifier] [showParticles] [ambient]");
                return true;
            case "remove":
                sendErrorMessage(sender, "Usage: /potion <remove> <index>");
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

    private static void sendErrorMessage(ICommandSender sender, String message) {
        TextComponentTranslation textComponent = new TextComponentTranslation(message);
        textComponent.getStyle().setColor(TextFormatting.RED);
        sender.sendMessage(textComponent);
    }

    private static void sendMessage(ICommandSender sender, String message, Object... args) {
        TextComponentTranslation textComponent = new TextComponentTranslation(message, args);
        textComponent.getStyle().setColor(TextFormatting.GREEN);
        sender.sendMessage(textComponent);
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