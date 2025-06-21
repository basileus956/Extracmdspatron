package com.binaris.extracmds;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Map;

@Mod.EventBusSubscriber
public class AnvilCustomBookHandler {

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();   // Typically the item (e.g. sword)
        ItemStack right = event.getRight(); // Typically the book (e.g. unsafe enchanted book)

        if (left == null || right == null) return;

        // Only trigger if the right item is an unsafe enchanted book
        if (!isCustomUnsafeBook(right)) return;

        // Don't allow book-to-book, book-to-book combining or book-to-item that's not valid
        if (left.getItem() == Items.ENCHANTED_BOOK || right.getItem() != Items.ENCHANTED_BOOK) return;
        if (!left.isItemEnchantable()) return;

        // Get stored enchants from the book
        NBTTagCompound tag = right.getTagCompound();
        if (tag == null || !tag.hasKey("StoredEnchantments", 9)) return;

        NBTTagList enchList = tag.getTagList("StoredEnchantments", 10);
        if (enchList.tagCount() == 0) return;

        ItemStack output = left.copy();
        Map<Enchantment, Integer> current = EnchantmentHelper.getEnchantments(output);

        for (int i = 0; i < enchList.tagCount(); i++) {
            NBTTagCompound enchTag = enchList.getCompoundTagAt(i);
            Enchantment ench = Enchantment.getEnchantmentByID(enchTag.getShort("id"));
            int level = enchTag.getShort("lvl");

            // Only apply enchantments that are compatible with this item
            if (ench == null || !ench.canApply(left)) continue;

            int currentLevel = current.getOrDefault(ench, 0);

            // ONLY overwrite if the book's level is higher (prevents unsafe upgrades stacking forever)
            if (level > currentLevel) {
                current.put(ench, level);
            }
        }

        EnchantmentHelper.setEnchantments(current, output);
        event.setOutput(output);
        event.setCost(1); // Default cost, you can change it if you want
    }

    // Identify if the book has the custom UnsafeBook tag
    private static boolean isCustomUnsafeBook(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag != null && tag.hasKey("UnsafeBook") && tag.getBoolean("UnsafeBook");
    }
}
