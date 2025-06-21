package com.binaris.extracmds.command;

import net.minecraft.command.*;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.init.Items;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.loot.*;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.*;

@Mod.EventBusSubscriber
public class TagRodCommand extends CommandBase {

    private static final String ROOT_TAG = "CustomRodTag";

    @Override
    public String getName() {
        return "tagrod";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "commands.extracmds.tagrod.usage";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!(sender instanceof EntityPlayer))
            throw new WrongUsageException("commands.extracmds.usage.needplayer");

        EntityPlayer player = (EntityPlayer) sender;
        ItemStack rod = player.getHeldItemMainhand();
        if (rod.getItem() != Items.FISHING_ROD)
            throw new CommandException("commands.extracmds.usage.needitem");

        NBTTagCompound root = rod.getTagCompound();
        if (root == null) root = new NBTTagCompound();
        if (args.length == 0)
            throw new WrongUsageException("commands.extracmds.tagrod.usage");

        NBTTagList entries = root.hasKey(ROOT_TAG) ? root.getTagList(ROOT_TAG, 10) : new NBTTagList();

        switch (args[0]) {
            case "booster": {
                if (args.length != 1)
                    throw new WrongUsageException("commands.extracmds.tagrod.usage.booster");
                boolean current = root.getBoolean("Booster");
                if (current) root.removeTag("Booster");
                else root.setBoolean("Booster", true);

                if (entries.tagCount() == 0 && root.hasKey(ROOT_TAG)) {
                    root.removeTag(ROOT_TAG);
                }

                maybeClearNBT(rod, root);
                sender.sendMessage(new TextComponentTranslation(current ?
                        "commands.extracmds.tagrod.unset_booster" :
                        "commands.extracmds.tagrod.set_booster"));
                break;
            }

            case "item": {
                if (args.length < 2)
                    throw new WrongUsageException("commands.extracmds.tagrod.usage.item");

                ResourceLocation id = new ResourceLocation(args[1]);
                if (!ForgeRegistries.ITEMS.containsKey(id))
                    throw new CommandException("commands.extracmds.tagrod.unknown_item", id);
                Item item = ForgeRegistries.ITEMS.getValue(id);

                int count = 1, meta = 0;
                float chance = -1f;
                String json = null;

                int i = 2;
                if (i < args.length && isNumeric(args[i])) count = Integer.parseInt(args[i++]);
                if (i < args.length && isNumeric(args[i])) meta = Integer.parseInt(args[i++]);
                if (i < args.length && isFloat(args[i])) chance = Math.max(0f, Float.parseFloat(args[i++]));
                if (i < args.length) json = String.join(" ", Arrays.copyOfRange(args, i, args.length));

                ItemStack stack = new ItemStack(item, count, meta);
                if (json != null) {
                    try {
                        stack.setTagCompound(JsonToNBT.getTagFromJson(json));
                    } catch (NBTException e) {
                        throw new CommandException("commands.extracmds.tagrod.invalid_nbt", e.getMessage());
                    }
                }

                NBTTagCompound newEntry = new NBTTagCompound();
                newEntry.setString("Type", "item");
                newEntry.setTag("Item", stack.writeToNBT(new NBTTagCompound()));
                if (chance >= 0f) newEntry.setFloat("Chance", chance);

                for (int j = 0; j < entries.tagCount(); j++) {
                    NBTTagCompound existing = entries.getCompoundTagAt(j);
                    if (existing.getString("Type").equals("item") &&
                            existing.getCompoundTag("Item").equals(newEntry.getCompoundTag("Item"))) {
                        entries.removeTag(j);
                        break;
                    }
                }

                entries.appendTag(newEntry);
                break;
            }
            case "loot": {
                if (args.length < 2)
                    throw new WrongUsageException("commands.extracmds.tagrod.usage.loot");

                ResourceLocation id = new ResourceLocation(args[1]);
                LootTable table = ((WorldServer) player.world).getLootTableManager().getLootTableFromLocation(id);
                if (table == null || table == LootTable.EMPTY_LOOT_TABLE)
                    throw new CommandException("commands.extracmds.tagrod.unknown_loot", id);

                float chance = (args.length >= 3 && isFloat(args[2])) ? Math.max(0f, Float.parseFloat(args[2])) : -1f;

                for (int j = 0; j < entries.tagCount(); j++) {
                    NBTTagCompound entry = entries.getCompoundTagAt(j);
                    if (entry.getString("Type").equals("loot") &&
                            entry.getString("LootTable").equals(id.toString())) {
                        entries.removeTag(j);
                        break;
                    }
                }

                NBTTagCompound entry = new NBTTagCompound();
                entry.setString("Type", "loot");
                entry.setString("LootTable", id.toString());
                if (chance >= 0f) entry.setFloat("Chance", chance);
                entries.appendTag(entry);
                break;
            }

            case "remove": {
                if (args.length == 1) {
                    root.removeTag(ROOT_TAG);
                    maybeClearNBT(rod, root);
                    sender.sendMessage(new TextComponentTranslation("commands.extracmds.tagrod.cleared"));
                    return;
                }

                if (!isNumeric(args[1]))
                    throw new WrongUsageException("commands.extracmds.tagrod.usage.remove");

                int index = Integer.parseInt(args[1]) - 1;
                if (index < 0 || index >= entries.tagCount())
                    throw new CommandException("commands.extracmds.tagrod.invalid_index");

                entries.removeTag(index);
                break;
            }

            default:
                throw new WrongUsageException(getUsage(sender));
        }

        if (entries.tagCount() > 0) {
            root.setTag(ROOT_TAG, entries);
        } else {
            root.removeTag(ROOT_TAG);
        }

        maybeClearNBT(rod, root);
    }

    private static void maybeClearNBT(ItemStack stack, NBTTagCompound root) {
        if (!root.hasKey(ROOT_TAG) && !root.hasKey("Booster")) {
            stack.setTagCompound(null);
        } else {
            stack.setTagCompound(root);
        }
    }
    @SubscribeEvent
    public static void onFish(ItemFishedEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        ItemStack rod = getHeldRod(player);
        if (rod.isEmpty() || !rod.hasTagCompound()) return;

        NBTTagList entries = rod.getTagCompound().getTagList(ROOT_TAG, 10);
        if (entries.tagCount() == 0) return;

        World world = player.world;
        List<EntityFishHook> hooks = world.getEntitiesWithinAABB(EntityFishHook.class, player.getEntityBoundingBox().grow(64), h -> h.getAngler() == player);
        EntityFishHook hook = hooks.isEmpty() ? null : hooks.get(0);
        if (hook == null || hook.isDead) return;

        boolean allExplicit = true;
        float totalSetChance = 0f;
        List<NBTTagCompound> validEntries = new ArrayList<>();
        List<Float> weights = new ArrayList<>();

        for (int i = 0; i < entries.tagCount(); i++) {
            NBTTagCompound entry = entries.getCompoundTagAt(i);
            if (!entry.hasKey("Chance")) {
                allExplicit = false;
            } else {
                float weight = entry.getFloat("Chance");
                if (weight > 0f) {
                    totalSetChance += weight;
                    validEntries.add(entry);
                    weights.add(weight);
                }
            }
        }

        if (allExplicit && totalSetChance > 0f && totalSetChance < 1f) {
            if (world.rand.nextFloat() > totalSetChance) return;

            float r = world.rand.nextFloat(), cumulative = 0f;
            float total = weights.stream().reduce(0f, Float::sum);
            List<Float> normalized = new ArrayList<>();
            for (float w : weights) normalized.add(w / total);

            event.setCanceled(true);
            event.getDrops().clear();

            for (int i = 0; i < validEntries.size(); i++) {
                cumulative += normalized.get(i);
                if (r <= cumulative) {
                    spawnDropFromEntry(world, player, hook, validEntries.get(i));
                    return;
                }
            }
        } else {
            List<Integer> unsetIndices = new ArrayList<>();
            validEntries.clear();
            weights.clear();
            totalSetChance = 0f;
            for (int i = 0; i < entries.tagCount(); i++) {
                NBTTagCompound tag = entries.getCompoundTagAt(i);
                if (tag.hasKey("Chance")) {
                    float w = tag.getFloat("Chance");
                    if (w > 0f) {
                        validEntries.add(tag);
                        weights.add(w);
                        totalSetChance += w;
                    }
                } else {
                    unsetIndices.add(i);
                }
            }

            float remaining = Math.max(1f - totalSetChance, 0f);
            float equalShare = unsetIndices.isEmpty() ? 0f : remaining / unsetIndices.size();

            for (int i : unsetIndices) {
                NBTTagCompound tag = entries.getCompoundTagAt(i);
                if (equalShare > 0f) {
                    validEntries.add(tag);
                    weights.add(equalShare);
                }
            }

            if (validEntries.isEmpty()) return;

            float r = world.rand.nextFloat(), cumulative = 0f;
            float total = weights.stream().reduce(0f, Float::sum);
            List<Float> normalized = new ArrayList<>();
            for (float w : weights) normalized.add(w / total);

            event.setCanceled(true);
            event.getDrops().clear();

            for (int i = 0; i < validEntries.size(); i++) {
                cumulative += normalized.get(i);
                if (r <= cumulative) {
                    spawnDropFromEntry(world, player, hook, validEntries.get(i));
                    return;
                }
            }
        }
    }

    private static void spawnDropFromEntry(World world, EntityPlayer player, EntityFishHook hook, NBTTagCompound entry) {
        ItemStack drop = ItemStack.EMPTY;
        if (entry.getString("Type").equals("item")) {
            drop = new ItemStack(entry.getCompoundTag("Item"));
        } else {
            String lootStr = entry.getString("LootTable");
            LootTable table = ((WorldServer) world).getLootTableManager().getLootTableFromLocation(new ResourceLocation(lootStr));
            LootContext ctx = new LootContext.Builder((WorldServer) world).withLuck(player.getLuck()).withPlayer(player).build();
            List<ItemStack> loot = table.generateLootForPools(world.rand, ctx);
            if (!loot.isEmpty()) drop = loot.get(0);
        }

        if (!drop.isEmpty()) {
            EntityItem entity = new EntityItem(world, hook.posX, hook.posY + 0.4D, hook.posZ, drop);
            double dx = player.posX - hook.posX;
            double dy = player.posY + 0.5 - hook.posY;
            double dz = player.posZ - hook.posZ;
            double speed = 0.25D;
            entity.motionX = dx * speed;
            entity.motionY = dy * speed + 0.1D;
            entity.motionZ = dz * speed;
            world.spawnEntity(entity);
        }
    }

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickItem event) {
        EntityPlayer player = event.getEntityPlayer();
        ItemStack rod = event.getItemStack();

        if (rod.getItem() != Items.FISHING_ROD || !rod.hasTagCompound()) return;
        if (!rod.getTagCompound().getBoolean("Booster")) return;

        EntityFishHook hook = player.fishEntity;
        if (hook == null || hook.isDead) return;

        double dx = hook.posX - player.posX;
        double dy = hook.posY - player.posY;
        double dz = hook.posZ - player.posZ;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist > 0.0D) {
            double speed = 1.5D;
            player.motionX = dx / dist * speed;
            player.motionY = dy / dist * speed + 0.4D;
            player.motionZ = dz / dist * speed;
            player.velocityChanged = true;
        }
    }
    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.getItem() != Items.FISHING_ROD || !stack.hasTagCompound()) return;

        NBTTagCompound tag = stack.getTagCompound();
        boolean hasBooster = tag.getBoolean("Booster");
        boolean hasCatchables = tag.hasKey(ROOT_TAG) && tag.getTagList(ROOT_TAG, 10).tagCount() > 0;

        if (!hasBooster && !hasCatchables) return;

        if (hasBooster)
            event.getToolTip().add(I18n.translateToLocal("commands.extracmds.tagrod.tooltip.booster"));

        if (hasCatchables) {
            NBTTagList entries = tag.getTagList(ROOT_TAG, 10);
            event.getToolTip().add(I18n.translateToLocal("commands.extracmds.tagrod.tooltip.header"));
            for (int i = 0; i < entries.tagCount(); i++) {
                NBTTagCompound t = entries.getCompoundTagAt(i);
                String display = "";
                if (t.getString("Type").equals("item")) {
                    ItemStack is = new ItemStack(t.getCompoundTag("Item"));
                    display = new TextComponentTranslation("commands.extracmds.tagrod.tooltip.item", is.getDisplayName()).getUnformattedText();
                } else if (t.getString("Type").equals("loot")) {
                    display = new TextComponentTranslation("commands.extracmds.tagrod.tooltip.loot", t.getString("LootTable")).getUnformattedText();
                }
                if (t.hasKey("Chance")) {
                    display += " " + new TextComponentTranslation("commands.extracmds.tagrod.tooltip.chance", (int) (t.getFloat("Chance") * 100)).getUnformattedText();
                }
                if (t.getString("Type").equals("item") && t.getCompoundTag("Item").hasKey("Count")) {
                    display += " " + new TextComponentTranslation("commands.extracmds.tagrod.tooltip.count", t.getCompoundTag("Item").getInteger("Count")).getUnformattedText();
                }
                event.getToolTip().add(display);
            }
        }
    }

    private static ItemStack getHeldRod(EntityPlayer player) {
        ItemStack main = player.getHeldItemMainhand();
        ItemStack off = player.getHeldItemOffhand();
        if (main.getItem() == Items.FISHING_ROD) return main;
        if (off.getItem() == Items.FISHING_ROD) return off;
        return ItemStack.EMPTY;
    }

    private static boolean isNumeric(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean isFloat(String s) {
        try {
            Float.parseFloat(s);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "booster", "item", "loot", "remove");
        } else if (args.length == 2 && args[0].equals("item")) {
            return getListOfStringsMatchingLastWord(args, ForgeRegistries.ITEMS.getKeys());
        } else if (args.length == 2 && args[0].equals("loot")) {
            return Collections.emptyList(); // Optional: return list of known loot tables
        }
        return Collections.emptyList();
    }
}
