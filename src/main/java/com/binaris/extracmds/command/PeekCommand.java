package com.binaris.extracmds.command;

import net.minecraft.command.*;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemShulkerBox;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.*;

@Mod.EventBusSubscriber
public class PeekCommand extends CommandBase {

    // Runtime-only cache to safely track original items per player
    private static final Map<UUID, ItemStack> peekCache = new HashMap<>();

    @Override
    public String getName() {
        return "peek";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "commands.extracmds.peek.usage";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        if (!(sender instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) sender;

        ItemStack held = player.getHeldItem(EnumHand.MAIN_HAND);
        if (!(held.getItem() instanceof ItemShulkerBox)) {
            player.sendMessage(new TextComponentTranslation("commands.extracmds.usage.needitem"));
            return;
        }

        // Load items from the shulker box's BlockEntityTag
        NBTTagCompound tag = held.getTagCompound();
        if (tag == null) tag = new NBTTagCompound();
        if (!tag.hasKey("BlockEntityTag")) {
            tag.setTag("BlockEntityTag", new NBTTagCompound());
        }

        NBTTagCompound blockEntityTag = tag.getCompoundTag("BlockEntityTag");
        NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
        ItemStackHelper.loadAllItems(blockEntityTag, items);

        // Show the chest GUI with those contents
        String displayName = held.hasDisplayName()
                ? held.getDisplayName()
                : new TextComponentTranslation(held.getUnlocalizedName() + ".name").getUnformattedText();

        InventoryBasic inventory = new InventoryBasic(displayName, false, items.size());
        for (int i = 0; i < items.size(); i++) {
            inventory.setInventorySlotContents(i, items.get(i));
        }

        // Open GUI
        player.displayGUIChest(inventory);

        // Store snapshot in memory cache
        peekCache.put(player.getUniqueID(), held.copy());
    }

    @SubscribeEvent
    public static void onContainerClose(PlayerContainerEvent.Close event) {
        if (!(event.getEntityPlayer() instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) event.getEntityPlayer();

        if (!(player.openContainer instanceof ContainerChest)) return;
        ContainerChest container = (ContainerChest) player.openContainer;
        IInventory inventory = container.getLowerChestInventory();
        if (!(inventory instanceof InventoryBasic)) return;

        UUID playerId = player.getUniqueID();
        if (!peekCache.containsKey(playerId)) return;

        ItemStack originalShulker = peekCache.get(playerId);
        if (!(originalShulker.getItem() instanceof ItemShulkerBox)) return;

        int slotCount = inventory.getSizeInventory();
        NonNullList<ItemStack> items = NonNullList.withSize(slotCount, ItemStack.EMPTY);
        for (int i = 0; i < slotCount; i++) {
            items.set(i, inventory.getStackInSlot(i));
        }

        // Store back into shulker's NBT
        NBTTagCompound tag = originalShulker.hasTagCompound()
                ? originalShulker.getTagCompound()
                : new NBTTagCompound();

        NBTTagCompound blockEntityTag = new NBTTagCompound();
        ItemStackHelper.saveAllItems(blockEntityTag, items);
        tag.setTag("BlockEntityTag", blockEntityTag);
        originalShulker.setTagCompound(tag);

        // Overwrite held item
        player.setHeldItem(EnumHand.MAIN_HAND, originalShulker);
        player.inventory.markDirty();
        player.sendContainerToPlayer(player.inventoryContainer);

        // Clear from memory
        peekCache.remove(playerId);
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos) {
        return Collections.emptyList();
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return sender.canUseCommand(4, getName());
    }
}
