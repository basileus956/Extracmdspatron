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
import net.minecraft.util.text.TextFormatting;

import java.util.ArrayList;
import java.util.List;

// This command is based in the original /potion command from the mod "MoreCommands",
// I just refactored it to be better to read and understand and added some new features.

// Silly warnings... o-o
@SuppressWarnings("NullableProblems")
public class PotionCommand extends CommandBase {
    @Override
    public String getName() {
        return "potion";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/potion <setcolour|settype|add|remove> <hex colour>/<type>/<effect>/<index> [duration] [amplifier] [show particles] [ambient] Add or remove an effect to your potion or set its colour. Duration should be an integer and defaults to 60, amplifier should be a byte (int with a max value of 127) and defaults to 0, index-based, show-particles should be a boolean and defaults to true and ambient (semi-show particles) should be a boolean and defaults to false. E.g. /potion setcolour #000000 OR /potion settype minecraft:long_night_vision OR /potion add minecraft:fire_resistance 120 1 false true OR /potion remove 1.";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        // Check if the arguments are valid
        if (args.length < 2 || !Lists.newArrayList("setcolour", "setcolor", "settype", "add", "remove").contains(args[0])){
            boolean handled = handleUsageError(sender, args);

            if(handled) return;
            else {
                sendErrorMessage(sender, "Usage: /potion <setcolour/setcolor> <colour>");
                sendErrorMessage(sender, "Usage: /potion <settype> <type>");
                sendErrorMessage(sender, "Usage: /potion <add> <effect> [duration] [amplifier] [showParticles] [ambient]");
                sendErrorMessage(sender, "Usage: /potion <remove> <index>");
                return;
            }
        }

        // Check if the sender is a player
        else if (!(sender instanceof EntityLivingBase)) {
            sendErrorMessage(sender, "Only living entities may use this command.");
            return;
        }

        // Check if the sender is holding a potion item
        ItemStack held = ((EntityLivingBase) sender).getHeldItemMainhand();
        if (held.getItem() != Items.POTIONITEM && held.getItem() != Items.TIPPED_ARROW && held.getItem() != Items.LINGERING_POTION && held.getItem() != Items.SPLASH_POTION) {
            sendErrorMessage(sender, "You must be holding a potion item, tipped arrow, lingering potion or a splash potion item for this command to work.");
            return;
        }

        // Check the subcommand
        switch (args[0]) {
            // Set the colour of the potion feature
            case "setcolour":
            case "setcolor":
                if (!isInteger(args[1], 16) && !isInteger(args[1].substring(1), 16))
                    sendErrorMessage(sender, "The given colour was invalid.");
                else {
                    NBTTagCompound nbt = MoreObjects.firstNonNull(held.getTagCompound(), new NBTTagCompound());
                    nbt.setInteger("CustomPotionColor", Integer.parseInt(args[1].startsWith("#") ? args[1].substring(1) : args[1], 16));
                    held.setTagCompound(nbt);
                    sendMessage(sender, "The colour of your potion has been set.");
                }
                break;


            // Set the type of the potion feature
            case "settype":
                PotionType type = PotionType.REGISTRY.getObject(new ResourceLocation(args[1]));
                if (type.getRegistryName().equals(new ResourceLocation("minecraft:empty")))
                    sendMessage(sender, "The given type could not be found.");
                else {
                    PotionUtils.addPotionToItemStack(held, type);
                    sendMessage(sender, "Your potion's type has been set.");
                }
                break;


            //  Add an effect to the potion feature
            case "add":
                ResourceLocation loc = new ResourceLocation(args[1]);
                Potion potion = null;
                for (Potion pot : Iterators.toArray(Potion.REGISTRY.iterator(), Potion.class))
                    if (pot.getRegistryName().equals(loc)) {
                        potion = pot;
                        break;
                    }
                if (potion == null)
                    sendErrorMessage(sender, TextFormatting.RED + "The given effect could not be found.");
                else {
                    int duration = args.length >= 3 && isInteger(args[2]) ? Integer.parseInt(args[2]) * 20 : 60 * 20;
                    int amplifier = args.length >= 4 && isInteger(args[3]) ? Math.min(Integer.parseInt(args[3]), Byte.MAX_VALUE) : 0;
                    boolean showParticles = args.length >= 5 && isInteger(args[4]) ? Boolean.parseBoolean(args[4]) : true;
                    boolean ambient = args.length >= 6 && isInteger(args[5]) ? Boolean.parseBoolean(args[5]) : false;
                    NBTTagList oldTagList = MoreObjects.firstNonNull(held.getTagCompound(), new NBTTagCompound()).getTagList("CustomPotionEffects", 10).copy();
                    PotionUtils.appendEffects(held, Lists.newArrayList(new PotionEffect(potion, duration, amplifier, ambient, showParticles)));
                    NBTTagList newTagList = MoreObjects.firstNonNull(held.getTagCompound(), new NBTTagCompound()).getTagList("CustomPotionEffects", 10);
                    oldTagList.forEach(nbt -> newTagList.appendTag(nbt)); // There seems to be an internal bug where it gets the tag list with a
                    // type of 9 even though its type is 10, so we fix that here.
                    sendMessage(sender, "Successfully added the effect to your potion.");
                }
                break;


            // Remove an effect from the potion feature
            case "remove":
                NBTTagList list = MoreObjects.firstNonNull(held.getTagCompound(), new NBTTagCompound()).getTagList("CustomPotionEffects", 10);
                if (!isInteger(args[1])) sendErrorMessage(sender, "The given index was not an integer.");
                else if (Integer.parseInt(args[1]) <= 0) sendErrorMessage(sender, "Index cannot be 0 or less.");
                else if (Integer.parseInt(args[1]) > list.tagCount())
                    sendErrorMessage(sender, "The given index (" + Integer.parseInt(args[1]) + ") was greater than the amount of custom effects on your potion (" + list.tagCount() + ").");
                else {
                    PotionEffect effect = PotionEffect.readCustomPotionEffectFromNBT((NBTTagCompound) list.get(Integer.parseInt(args[1]) - 1));
                    list.removeTag(Integer.parseInt(args[1]) - 1);
                    sendMessage(sender, "Effect " + Integer.parseInt(args[1]) + " of type " + effect.getPotion().getRegistryName() + " with a duration of " + effect.getDuration() / 20 + " seconds and an amplifier of " + effect.getAmplifier() + " which did " + (effect.doesShowParticles() ? "" : "not ") + "show particles and was" + (effect.getIsAmbient() ? "" : "n't") + " ambient has been removed.");
                }
                break;
        }
    }

    private static boolean handleUsageError(ICommandSender sender, String[] args) {
        if(!(args.length >= 1)) return false;

        if(args[0].equals("setcolour") || args[0].equals("setcolor")) {
            sendErrorMessage(sender, "Usage: /potion <setcolour/setcolor> <colour>");
            return true;
        } else if(args[0].equals("settype")) {
            sendErrorMessage(sender, "Usage: /potion <settype> <type>");
            return true;
        } else if(args[0].equals("add")) {
            sendErrorMessage(sender, "Usage: /potion <add> <effect> [duration] [amplifier] [showParticles] [ambient]");
            return true;
        } else if(args[0].equals("remove")) {
            sendErrorMessage(sender, "Usage: /potion <remove> <index>");
            return true;
        }

        return false;
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos) {
        return args.length == 1 ?
                getListOfStringsMatchingLastWord(args, Lists.newArrayList("setcolour", "settype", "add", "remove"))
                : args.length == 2 ? args[0].equals("add") ? getListOfStringsMatchingLastWord(args, Potion.REGISTRY.getKeys()) :
                args[0].equals("settype") ? getListOfStringsMatchingLastWord(args, PotionType.REGISTRY.getKeys()) : new ArrayList() :
                new ArrayList();
    }

    private static void sendErrorMessage(ICommandSender sender, String message) {
        TextComponentString textComponent = new TextComponentString(message);
        textComponent.getStyle().setColor(TextFormatting.RED);
        sender.sendMessage(textComponent);
    }

    private static void sendMessage(ICommandSender sender, String message) {
        TextComponentString textComponent = new TextComponentString(message);
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
