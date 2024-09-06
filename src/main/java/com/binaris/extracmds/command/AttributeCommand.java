package com.binaris.extracmds.command;

import com.binaris.extracmds.CommandUtil;
import com.binaris.extracmds.ExtraCMDS;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nullable;
import java.util.*;

// Silly warnings... o-o
@SuppressWarnings("NullableProblems")
public class AttributeCommand extends CommandBase {
    private static final HashMap<String, UUID> attributeUUIDs = new HashMap<>();

    public AttributeCommand() {
        attributeUUIDs.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(), UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF"));
        attributeUUIDs.put(SharedMonsterAttributes.ATTACK_SPEED.getName(), UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3"));
        attributeUUIDs.put(SharedMonsterAttributes.MAX_HEALTH.getName(), UUID.fromString("5D6F0BA2-1186-46AC-B896-C61C5CEE99CC"));
        attributeUUIDs.put(SharedMonsterAttributes.LUCK.getName(), UUID.fromString("03C3C89D-7037-4B42-869F-B146BCB64D2E"));
        attributeUUIDs.put(SharedMonsterAttributes.ARMOR_TOUGHNESS.getName(), UUID.fromString("D8499B04-0E66-4726-AB29-64469D734E0D"));
        attributeUUIDs.put(SharedMonsterAttributes.ARMOR.getName(), UUID.fromString("CC7C1AAF-8FF5-4396-ACD1-fC7A7E3413E5"));
        attributeUUIDs.put(SharedMonsterAttributes.FOLLOW_RANGE.getName(), UUID.fromString("9F3BF2D7-6A6A-4EE3-B5A5-70B5AB0E0656"));
        attributeUUIDs.put(SharedMonsterAttributes.KNOCKBACK_RESISTANCE.getName(), UUID.fromString("2AD3F246-FEE1-4E67-B886-69FD380BB150"));
        attributeUUIDs.put(SharedMonsterAttributes.MOVEMENT_SPEED.getName(), UUID.fromString("A7E29D4B-4E2C-4A90-AE8E-A7CD9A4B9B8A"));
        attributeUUIDs.put(SharedMonsterAttributes.FLYING_SPEED.getName(), UUID.fromString("E199AD21-BA8A-4C53-8D13-6182D5C69D3A"));

        CommandUtil.wizardryUtilAttributes(attributeUUIDs);
    }

    @Override
    public String getName() {
        return "attribute";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/attribute [slot] <attribute> <amount>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 2 || args.length > 3) {
            throw new CommandException("Invalid command usage. Usage: " + getUsage(sender));
        }

        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        ItemStack itemStack = player.getHeldItemMainhand();

        // Check if the player is holding an item
        if (itemStack.isEmpty()) {
            throw new CommandException("You must be holding an item to use this command.");
        }


        // Start parsing the slot argument
        String slotString = args[0];

        // Start parsing the attribute argument
        String attributeName = args[args.length - 2];
        UUID attributeUUID = attributeUUIDs.get(attributeName);
        if (attributeUUID == null) {
            throw new CommandException("Invalid attribute name.");
        }

        // Start parsing the amount argument
        double amount;
        try {
            amount = Double.parseDouble(args[args.length - 1]);
        } catch (NumberFormatException e) {
            throw new CommandException("Invalid amount format.");
        }

        // If any attribute with the same name is already applied, remove it
        boolean removed = removeAttribute(itemStack, attributeName);
        if(removed){
            ExtraCMDS.logger.info("Removed attribute: " + attributeName + " when applying new attribute.");
        }

        // Apply the attribute
        if (Objects.equals(slotString, "all")) {
            // Apply attribute to all slots
            for (EntityEquipmentSlot s : EntityEquipmentSlot.values()) {
                applyAttribute(itemStack, s, attributeUUID, attributeName, amount);
            }
        } else {
            // Parse string to slot
            EntityEquipmentSlot slot = getEquipmentSlotByName(slotString);
            if (slot == null) {
                throw new CommandException("Invalid slot name.");
            }

            // Apply attribute to specific slotString
            applyAttribute(itemStack, slot, attributeUUID, attributeName, amount);
        }

        if (server.sendCommandFeedback()) {
            ITextComponent textComponent = new TextComponentString("Attribute " + attributeName + " added with amount " + amount);
            textComponent.getStyle().setColor(TextFormatting.GREEN);
            sender.sendMessage(textComponent);
        }

        player.inventory.markDirty();
    }

    private boolean removeAttribute(ItemStack itemStack, String attributeName) {
        if(!itemStack.hasTagCompound()) itemStack.setTagCompound(new NBTTagCompound());

        if (!itemStack.getTagCompound().hasKey("AttributeModifiers", 9))
            itemStack.getTagCompound().setTag("AttributeModifiers", new NBTTagList());

        NBTTagList nbttaglist = itemStack.getTagCompound().getTagList("AttributeModifiers", 10);
        boolean found = false;
        for (int i = 0; i < nbttaglist.tagCount(); i++) {
            NBTTagCompound nbttagcompound = nbttaglist.getCompoundTagAt(i);
            if (nbttagcompound.getString("AttributeName").equals(attributeName)) {
                nbttaglist.removeTag(i);
                found = true;
            }
        }
        return found;
    }

    private void applyAttribute(ItemStack itemStack, EntityEquipmentSlot slot, UUID attributeUUID, String attributeName, double amount) {
        // Create or update the attribute modifier
        AttributeModifier modifier = new AttributeModifier(attributeUUID, attributeName, amount, 0);
        secureAddAttribute(itemStack, slot, modifier, attributeName);
    }

    private static void secureAddAttribute(ItemStack itemStack, EntityEquipmentSlot slot, AttributeModifier modifier, String attributeName) {
        itemStack.addAttributeModifier(attributeName, modifier, slot);

        if (!itemStack.getTagCompound().hasKey("AttributeModifiers", 9)) {
            itemStack.getTagCompound().setTag("AttributeModifiers", new NBTTagList());
        }

        NBTTagList nbttaglist = itemStack.getTagCompound().getTagList("AttributeModifiers", 10);
        NBTTagCompound nbttagcompound = SharedMonsterAttributes.writeAttributeModifierToNBT(modifier);
        nbttagcompound.setString("AttributeName", attributeName);

        if (slot != null) {
            nbttagcompound.setString("Slot", slot.getName());
        }

        for (int i = 0; i < nbttaglist.tagCount(); ++i) {
            NBTTagCompound nbttagcompound1 = nbttaglist.getCompoundTagAt(i);

            if (nbttagcompound1.getString("AttributeName").equals(attributeName) && nbttagcompound1.getString("Slot").equals(slot.getName())) {
                nbttaglist.removeTag(i);
                break;
            }
        }

        nbttaglist.appendTag(nbttagcompound);
    }

    @Nullable
    private EntityEquipmentSlot getEquipmentSlotByName(String name) {
        for (EntityEquipmentSlot slot : EntityEquipmentSlot.values()) {
            if (slot.getName().equalsIgnoreCase(name)) {
                return slot;
            }
        }
        return null;
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            List<String> slots = new ArrayList<>();
            for (EntityEquipmentSlot slot : EntityEquipmentSlot.values()) {
                slots.add(slot.getName());
            }
            slots.add("all");
            return getListOfStringsMatchingLastWord(args, slots);
        } else if (args.length == 2) {
            return getListOfStringsMatchingLastWord(args, attributeUUIDs.keySet());
        } else {
            return Collections.emptyList();
        }
    }
}

