package com.binaris.extracmds.command;

import net.minecraft.command.*;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nullable;
import java.util.*;

public class BookCommand extends CommandBase {

    private static final List<String> GENERATIONS = Arrays.asList("original", "copy_of_original", "copy_of_copy", "tattered");

    @Override
    public String getName() {
        return "book";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "commands.extracmds.book.usage";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!(sender instanceof EntityPlayerMP)) {
            throw new CommandException("commands.extracmds.usage.needplayer");
        }

        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        ItemStack heldItem = player.getHeldItemMainhand();

        if (heldItem.isEmpty()) {
            throw new CommandException("commands.extracmds.usage.needitem");
        }

        if (args.length < 1) {
            throw new WrongUsageException(getUsage(sender));
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "seal": {
                if (heldItem.getItem() != Items.WRITABLE_BOOK)
                    throw new CommandException("commands.extracmds.usage.needitem");

                NBTTagCompound tag = heldItem.getTagCompound();
                if (tag == null || !tag.hasKey("pages", 9) || tag.getTagList("pages", 8).tagCount() == 0) {
                    throw new CommandException("commands.extracmds.book.seal.empty");
                }

                NBTTagCompound newTag = tag.copy();
                if (!newTag.hasKey("title", 8)) newTag.setString("title", "Untitled");
                if (!newTag.hasKey("author", 8)) newTag.setString("author", sender.getName());

                NBTTagList originalPages = newTag.getTagList("pages", 8);
                NBTTagList jsonPages = new NBTTagList();
                for (int i = 0; i < originalPages.tagCount(); i++) {
                    String plain = originalPages.getStringTagAt(i);
                    String json = ITextComponent.Serializer.componentToJson(new TextComponentString(plain));
                    jsonPages.appendTag(new NBTTagString(json));
                }
                newTag.setTag("pages", jsonPages);

                ItemStack newBook = new ItemStack(Items.WRITTEN_BOOK);
                NBTTagCompound merged = heldItem.getTagCompound() != null ? heldItem.getTagCompound().copy() : new NBTTagCompound();
                merged.merge(newTag);
                newBook.setTagCompound(merged);
                newBook.setCount(heldItem.getCount());
                player.setHeldItem(EnumHand.MAIN_HAND, newBook);
                sender.sendMessage(new TextComponentTranslation("commands.extracmds.book.seal.success"));
                break;
            }

            case "unseal": {
                if (heldItem.getItem() != Items.WRITTEN_BOOK)
                    throw new CommandException("commands.extracmds.usage.needitem");

                NBTTagCompound tag = heldItem.getTagCompound();
                NBTTagCompound copy = tag != null ? tag.copy() : new NBTTagCompound();

                if (copy.hasKey("pages", 9)) {
                    NBTTagList pages = copy.getTagList("pages", 8);
                    NBTTagList rawPages = new NBTTagList();
                    for (int i = 0; i < pages.tagCount(); i++) {
                        try {
                            ITextComponent component = ITextComponent.Serializer.jsonToComponent(pages.getStringTagAt(i));
                            rawPages.appendTag(new NBTTagString(component.getUnformattedText()));
                        } catch (Exception e) {
                            rawPages.appendTag(new NBTTagString(""));
                        }
                    }
                    copy.setTag("pages", rawPages);
                }

                copy.removeTag("title");
                copy.removeTag("author");
                copy.removeTag("generation");
                copy.removeTag("resolved");

                ItemStack newBook = new ItemStack(Items.WRITABLE_BOOK);
                NBTTagCompound merged = copy;
                NBTTagCompound original = heldItem.getTagCompound();

                if (original != null) {
                    for (String key : Arrays.asList("display", "ench", "AttributeModifiers", "HideFlags", "Unbreakable")) {
                        if (original.hasKey(key)) {
                            merged.setTag(key, original.getTag(key));
                        }
                    }
                }

                newBook.setTagCompound(merged);
                newBook.setCount(heldItem.getCount());
                player.setHeldItem(EnumHand.MAIN_HAND, newBook);
                sender.sendMessage(new TextComponentTranslation("commands.extracmds.book.unseal.success"));
                break;
            }

            case "title": {
                if (heldItem.getItem() != Items.WRITTEN_BOOK)
                    throw new CommandException("commands.extracmds.usage.needitem");

                if (args.length < 2)
                    throw new WrongUsageException("commands.extracmds.book.usage.title");

                String title = buildString(args, 1);
                if (title.length() > 32)
                    throw new CommandException("commands.extracmds.book.title.toolong");

                NBTTagCompound tag = heldItem.getTagCompound() != null ? heldItem.getTagCompound().copy() : new NBTTagCompound();
                tag.setString("title", title);
                heldItem.setTagCompound(tag);
                sender.sendMessage(new TextComponentTranslation("commands.extracmds.book.settitle", title));
                break;
            }

            case "author": {
                if (heldItem.getItem() != Items.WRITTEN_BOOK)
                    throw new CommandException("commands.extracmds.usage.needitem");

                if (args.length < 2)
                    throw new WrongUsageException("commands.extracmds.book.usage.author");

                String author = buildString(args, 1);
                NBTTagCompound tag = heldItem.getTagCompound() != null ? heldItem.getTagCompound().copy() : new NBTTagCompound();
                tag.setString("author", author);
                heldItem.setTagCompound(tag);
                sender.sendMessage(new TextComponentTranslation("commands.extracmds.book.setauthor", author));
                break;
            }

            case "generation": {
                if (args.length < 2 || !GENERATIONS.contains(args[1])) {
                    throw new WrongUsageException("commands.extracmds.book.usage.generation");
                }

                int gen = GENERATIONS.indexOf(args[1]);
                NBTTagCompound tag = heldItem.getTagCompound() != null ? heldItem.getTagCompound().copy() : new NBTTagCompound();
                tag.setInteger("generation", gen);
                heldItem.setTagCompound(tag);
                sender.sendMessage(new TextComponentTranslation("commands.extracmds.book.setgeneration", args[1]));
                break;
            }

            case "enchant": {
                if (args.length < 2)
                    throw new WrongUsageException("commands.extracmds.book.usage.enchant");

                ResourceLocation enchantID = new ResourceLocation(args[1].toLowerCase());
                Enchantment enchantment = Enchantment.REGISTRY.getObject(enchantID);
                if (enchantment == null)
                    throw new CommandException("commands.enchant.notFound", args[1]);

                int level = args.length >= 3 ? parseInt(args[2]) : 1;

                ItemStack result = heldItem;

                if (heldItem.getItem() == Items.BOOK) {
                    result = new ItemStack(Items.ENCHANTED_BOOK);
                    NBTTagCompound merged = heldItem.getTagCompound() != null ? heldItem.getTagCompound().copy() : new NBTTagCompound();
                    result.setTagCompound(merged);
                    result.setCount(heldItem.getCount());
                    player.setHeldItem(player.getActiveHand(), result);
                } else if (heldItem.getItem() != Items.ENCHANTED_BOOK) {
                    throw new CommandException("commands.extracmds.usage.needitem");
                }

                NBTTagCompound tag = result.getTagCompound();
                NBTTagList stored = tag.getTagList("StoredEnchantments", 10);
                NBTTagList newStored = new NBTTagList();
                boolean updated = false;

                for (int i = 0; i < stored.tagCount(); i++) {
                    NBTTagCompound enchTag = stored.getCompoundTagAt(i);
                    int id = enchTag.getShort("id");
                    if (Enchantment.getEnchantmentByID(id) == enchantment) {
                        NBTTagCompound newTag = new NBTTagCompound();
                        newTag.setShort("id", (short) id);
                        newTag.setShort("lvl", (short) level);
                        newStored.appendTag(newTag);
                        updated = true;
                    } else {
                        newStored.appendTag(enchTag);
                    }
                }

                if (!updated) {
                    NBTTagCompound newTag = new NBTTagCompound();
                    newTag.setShort("id", (short) Enchantment.getEnchantmentID(enchantment));
                    newTag.setShort("lvl", (short) level);
                    newStored.appendTag(newTag);
                }

                tag.setTag("StoredEnchantments", newStored);
                tag.setBoolean("UnsafeBook", true);
                result.setTagCompound(tag);

                sender.sendMessage(new TextComponentTranslation("commands.enchant.success"));
                break;
            }

            case "unenchant": {
                if (heldItem.getItem() != Items.ENCHANTED_BOOK)
                    throw new CommandException("commands.extracmds.usage.needitem");

                NBTTagCompound tag = heldItem.getTagCompound() != null ? heldItem.getTagCompound().copy() : new NBTTagCompound();
                if (!tag.hasKey("StoredEnchantments", 9))
                    throw new CommandException("commands.extracmds.book.unenchant.fail");

                if (args.length < 2) {
                    tag.removeTag("StoredEnchantments");
                    tag.removeTag("UnsafeBook"); // <-- Remove UnsafeBook when all enchants are gone!
                    if (tag.hasNoTags()) {
                        heldItem.setTagCompound(null);
                    } else {
                        heldItem.setTagCompound(tag);
                    }
                    sender.sendMessage(new TextComponentTranslation("commands.extracmds.book.unenchant.all"));
                    return;
                }

                ResourceLocation enchantID = new ResourceLocation(args[1].toLowerCase());
                Enchantment enchantment = Enchantment.REGISTRY.getObject(enchantID);
                if (enchantment == null)
                    throw new CommandException("commands.enchant.notFound", args[1]);

                NBTTagList currentList = tag.getTagList("StoredEnchantments", 10);
                NBTTagList newList = new NBTTagList();
                for (int i = 0; i < currentList.tagCount(); i++) {
                    NBTTagCompound enchTag = currentList.getCompoundTagAt(i);
                    if (Enchantment.getEnchantmentByID(enchTag.getShort("id")) != enchantment) {
                        newList.appendTag(enchTag);
                    }
                }

                if (currentList.tagCount() == newList.tagCount())
                    throw new CommandException("commands.extracmds.book.unenchant.fail");

                if (newList.tagCount() == 0) {
                    tag.removeTag("StoredEnchantments");
                    tag.removeTag("UnsafeBook"); // <-- Remove UnsafeBook when last enchant is removed!
                } else {
                    tag.setTag("StoredEnchantments", newList);
                }

                if (tag.hasNoTags()) {
                    heldItem.setTagCompound(null);
                } else {
                    heldItem.setTagCompound(tag);
                }

                sender.sendMessage(new TextComponentTranslation("commands.extracmds.book.unenchant.success", new TextComponentTranslation(enchantment.getName())));
                break;
            }

            default:
                throw new WrongUsageException(getUsageKeyFor(args));
        }
    }

    private String getUsageKeyFor(String[] args) {
        if (args.length == 0) return getUsage(null);
        switch (args[0].toLowerCase()) {
            case "title": return "commands.extracmds.book.usage.title";
            case "author": return "commands.extracmds.book.usage.author";
            case "generation": return "commands.extracmds.book.usage.generation";
            case "enchant": return "commands.extracmds.book.usage.enchant";
            case "unenchant": return "commands.extracmds.book.usage.unenchant";
            case "seal": return "commands.extracmds.book.usage.seal";
            case "unseal": return "commands.extracmds.book.usage.unseal";
            default: return "commands.extracmds.book.usage";
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos pos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "title", "author", "generation", "enchant", "unenchant", "seal", "unseal");
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("generation")) {
                return getListOfStringsMatchingLastWord(args, GENERATIONS);
            }
            if (args[0].equalsIgnoreCase("enchant")) {
                return getListOfStringsMatchingLastWord(args, Enchantment.REGISTRY.getKeys());
            }
            if (args[0].equalsIgnoreCase("unenchant")) {
                List<String> matches = new ArrayList<>();
                EntityPlayerMP player;
                try {
                    player = getCommandSenderAsPlayer(sender);
                } catch (Exception e) {
                    return Collections.emptyList();
                }

                ItemStack heldItem = player.getHeldItemMainhand();
                if (heldItem.getItem() == Items.ENCHANTED_BOOK) {
                    NBTTagCompound tag = heldItem.getTagCompound();
                    if (tag != null && tag.hasKey("StoredEnchantments", 9)) {
                        NBTTagList list = tag.getTagList("StoredEnchantments", 10);
                        for (int i = 0; i < list.tagCount(); i++) {
                            NBTTagCompound ench = list.getCompoundTagAt(i);
                            Enchantment stored = Enchantment.getEnchantmentByID(ench.getShort("id"));
                            if (stored != null) {
                                ResourceLocation id = Enchantment.REGISTRY.getNameForObject(stored);
                                if (id != null) {
                                    matches.add(id.toString());
                                }
                            }
                        }
                    }
                }
                return matches;
            }
        }
        return Collections.emptyList();
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return sender.canUseCommand(2, getName());
    }
}
