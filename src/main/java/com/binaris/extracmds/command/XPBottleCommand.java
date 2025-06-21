package com.binaris.extracmds.command;

import net.minecraft.command.*;
import net.minecraft.entity.item.EntityExpBottle;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.resources.I18n;

import java.util.List;

@Mod.EventBusSubscriber
public class XPBottleCommand extends CommandBase {

    public static final String XP_TAG = "CustomXP";
    public static final String TYPE_TAG = "XPBottleType";
    public static final String IS_LEVEL_TAG = "IsLevelBottle";
    public static final String CUSTOM_STACK_TAG = "CustomXPBottle";

    @Override
    public String getName() {
        return "xpbottle";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "commands.extracmds.xpbottle.usage";
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "consumable", "throwable");
        }
        return super.getTabCompletions(server, sender, args, pos);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!(sender instanceof EntityPlayer)) {
            throw new CommandException("commands.extracmds.usage.needplayer");
        }
        EntityPlayer player = (EntityPlayer) sender;

        if (args.length < 2) throw new WrongUsageException(getUsage(sender));

        String type = args[0].toLowerCase();
        if (!type.equals("consumable") && !type.equals("throwable")) {
            throw new CommandException("commands.extracmds.xpbottle.invalid_type");
        }

        String rawAmount = args[1];
        int value;
        boolean isLevel = false;

        if (rawAmount.endsWith("l")) {
            value = parseInt(rawAmount.substring(0, rawAmount.length() - 1));
            isLevel = true;
        } else {
            value = parseInt(rawAmount);
        }

        int amount = args.length >= 3 ? parseInt(args[2]) : 1;

        for (int i = 0; i < amount; i++) {
            ItemStack stack = new ItemStack(Items.EXPERIENCE_BOTTLE);
            NBTTagCompound tag = new NBTTagCompound();

            tag.setInteger(XP_TAG, value);
            tag.setString(TYPE_TAG, type);
            tag.setBoolean(IS_LEVEL_TAG, isLevel);
            stack.setTagCompound(tag);

            stack.setStackDisplayName("§eExperience Bottle");
            player.inventory.addItemStackToInventory(stack);
        }

        String unit = isLevel ? "levels" : "XP";
        String localizedType = I18n.format("xpbottle.type." + type);
        player.sendMessage(new TextComponentTranslation("commands.extracmds.xpbottle.success", amount, localizedType, value, unit));
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.getItem() != Items.EXPERIENCE_BOTTLE) return;

        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey(XP_TAG) || !tag.hasKey(TYPE_TAG)) return;

        int value = tag.getInteger(XP_TAG);
        boolean isLevel = tag.getBoolean(IS_LEVEL_TAG);
        String kind = tag.getString(TYPE_TAG);
        String label = isLevel ? "Levels" : "XP";
        String localizedKind = I18n.format("xpbottle.type." + kind.toLowerCase());
        String formatted = I18n.format("tooltip.extracmds.xpbottle.stored", label, value, localizedKind);
        event.getToolTip().add(TextFormatting.GRAY + "" + TextFormatting.ITALIC + formatted);
    }

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickItem event) {
        ItemStack stack = event.getItemStack();
        if (stack.getItem() != Items.EXPERIENCE_BOTTLE) return;

        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey(XP_TAG) || !tag.hasKey(TYPE_TAG)) return;
        if (!"consumable".equalsIgnoreCase(tag.getString(TYPE_TAG))) return;

        if (event.getWorld().isRemote) return;

        EntityPlayerMP player = (EntityPlayerMP) event.getEntityPlayer();
        int value = tag.getInteger(XP_TAG);
        boolean isLevel = tag.getBoolean(IS_LEVEL_TAG);

        if (isLevel) {
            player.addExperienceLevel(value);
        } else if (value >= 0) {
            player.addExperience(value);
        } else {
            removeExperience(player, -value);
        }

        stack.shrink(1);
        player.world.playSound(null, player.getPositionVector().x, player.getPositionVector().y, player.getPositionVector().z,
                SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.6F, 1.0F);

        event.setCanceled(true);
        player.swingArm(event.getHand());
    }

    @SubscribeEvent
    public static void onBottleThrow(PlayerInteractEvent.RightClickItem event) {
        ItemStack stack = event.getItemStack();
        if (stack.getItem() != Items.EXPERIENCE_BOTTLE) return;

        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey(XP_TAG) || !tag.hasKey(TYPE_TAG)) return;
        if (!"throwable".equalsIgnoreCase(tag.getString(TYPE_TAG))) return;

        EntityPlayer player = event.getEntityPlayer();
        World world = player.world;
        EnumHand hand = event.getHand();

        event.setCanceled(true);

        if (!world.isRemote) {
            EntityExpBottle bottle = new EntityExpBottle(world, player);
            bottle.getEntityData().setTag(CUSTOM_STACK_TAG, stack.writeToNBT(new NBTTagCompound()));
            bottle.shoot(player, player.rotationPitch, player.rotationYaw, 0.0F, 1.1F, 0.5F);
            world.spawnEntity(bottle);
        }

        world.playSound(null, player.getPositionVector().x, player.getPositionVector().y, player.getPositionVector().z,
                SoundEvents.ENTITY_EXPERIENCE_BOTTLE_THROW, SoundCategory.PLAYERS,
                0.5F, 0.4F / (player.getRNG().nextFloat() * 0.4F + 0.8F));

        if (!player.capabilities.isCreativeMode) {
            stack.shrink(1);
        }

        player.swingArm(hand);
    }

    @SubscribeEvent
    public static void onBottleImpact(ProjectileImpactEvent.Throwable event) {
        if (!(event.getThrowable() instanceof EntityExpBottle)) return;
        EntityExpBottle bottle = (EntityExpBottle) event.getThrowable();

        NBTTagCompound entityTag = bottle.getEntityData();
        if (!entityTag.hasKey(CUSTOM_STACK_TAG)) return;

        ItemStack stack = new ItemStack(entityTag.getCompoundTag(CUSTOM_STACK_TAG));
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey(XP_TAG) || !tag.hasKey(TYPE_TAG)) return;
        if (!"throwable".equalsIgnoreCase(tag.getString(TYPE_TAG))) return;

        int value = tag.getInteger(XP_TAG);
        boolean isLevel = tag.getBoolean(IS_LEVEL_TAG);
        int xp = isLevel ? getXPForLevelGain(0, value + 1) : value;

        if (!bottle.world.isRemote) {
            World world = bottle.world;

            world.playSound(null, bottle.posX, bottle.posY, bottle.posZ,
                    SoundEvents.ENTITY_EXPERIENCE_BOTTLE_THROW,
                    SoundCategory.NEUTRAL, 0.5F,
                    0.4F / (world.rand.nextFloat() * 0.4F + 0.8F));

            world.playEvent(2002, new BlockPos(bottle), 0xAA00FF);
            bottle.setDead();

            while (xp > 0) {
                int split = EntityXPOrb.getXPSplit(xp);
                xp -= split;
                world.spawnEntity(new EntityXPOrb(world, bottle.posX, bottle.posY, bottle.posZ, split));
            }
        }

        event.setCanceled(true);
    }

    private static void removeExperience(EntityPlayerMP player, int amount) {
        int currentXP = getPlayerTotalXP(player);
        int newXP = Math.max(currentXP - amount, 0);
        player.experienceTotal = 0;
        player.experienceLevel = 0;
        player.experience = 0.0F;
        player.addExperience(newXP);
    }

    private static int getPlayerTotalXP(EntityPlayerMP player) {
        int level = player.experienceLevel;
        int base = getTotalXPForLevel(level);
        return base + Math.round(player.experience * player.xpBarCap());
    }

    private static int getXPForLevelGain(int currentLevel, int levelGain) {
        return getTotalXPForLevel(currentLevel + levelGain) - getTotalXPForLevel(currentLevel);
    }

    private static int getTotalXPForLevel(int level) {
        if (level <= 16) return level * level + 6 * level;
        if (level <= 31) return (int)(2.5 * level * level - 40.5 * level + 360);
        return (int)(4.5 * level * level - 162.5 * level + 2220);
    }
}
