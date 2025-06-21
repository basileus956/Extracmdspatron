package com.binaris.extracmds.command;

import com.binaris.extracmds.CommandUtil;
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
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nullable;
import java.util.*;

public class AttributeCommand extends CommandBase {
    private static final HashMap<String, UUID> attributeUUIDs = new HashMap<>();

    private static final Map<String, Integer> OPERATION_MAP = new HashMap<>();
    static {
        OPERATION_MAP.put("addition", 0);
        OPERATION_MAP.put("multiply_base", 1);
        OPERATION_MAP.put("multiply_total", 2);
        OPERATION_MAP.put("0", 0);
        OPERATION_MAP.put("1", 1);
        OPERATION_MAP.put("2", 2);
    }

    public AttributeCommand() {
        attributeUUIDs.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(), UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF"));
        attributeUUIDs.put(SharedMonsterAttributes.ATTACK_SPEED.getName(), UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3"));
        attributeUUIDs.put(SharedMonsterAttributes.MAX_HEALTH.getName(), UUID.fromString("5D6F0BA2-1186-46AC-B896-C61C5CEE99CC"));
        attributeUUIDs.put(SharedMonsterAttributes.LUCK.getName(), UUID.fromString("03C3C89D-7037-4B42-869F-B146BCB64D2E"));
        attributeUUIDs.put(SharedMonsterAttributes.ARMOR_TOUGHNESS.getName(), UUID.fromString("D8499B04-0E66-4726-AB29-64469D734E0D"));
        attributeUUIDs.put(SharedMonsterAttributes.ARMOR.getName(), UUID.fromString("CC7C1AAF-8FF5-4396-ACD1-FC7A7E3413E5"));
        attributeUUIDs.put(SharedMonsterAttributes.FOLLOW_RANGE.getName(), UUID.fromString("9F3BF2D7-6A6A-4EE3-B5A5-70B5AB0E0656"));
        attributeUUIDs.put(SharedMonsterAttributes.KNOCKBACK_RESISTANCE.getName(), UUID.fromString("2AD3F246-FEE1-4E67-B886-69FD380BB150"));
        attributeUUIDs.put(SharedMonsterAttributes.MOVEMENT_SPEED.getName(), UUID.fromString("A7E29D4B-4E2C-4A90-AE8E-A7CD9A4B9B8A"));

        CommandUtil.wizardryUtilAttributes(attributeUUIDs);
        CommandUtil.forgeAttributes(attributeUUIDs);
    }

    @Override
    public String getName() {
        return "attribute";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "commands.extracmds.attribute.usage";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 3 || args.length > 4) {
            throw new CommandException("commands.extracmds.usage.error", new Object[]{new TextComponentTranslation(getUsage(sender))});
        }

        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        ItemStack itemStack = player.getHeldItemMainhand();

        if (itemStack.isEmpty()) {
            throw new CommandException("commands.extracmds.attribute.noItem");
        }

        String slotString = args[0];
        String attributeName = args[1];

        if (!attributeUUIDs.containsKey(attributeName)) {
            throw new CommandException("commands.extracmds.attribute.invalidAttribute");
        }

        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            throw new CommandException("commands.extracmds.attribute.invalidAmount");
        }

        int operation = 0;
        if (args.length == 4) {
            String opString = args[3].toLowerCase();
            if (!OPERATION_MAP.containsKey(opString)) {
                throw new CommandException("commands.extracmds.attribute.invalidOperation");
            }
            operation = OPERATION_MAP.get(opString);
        }

        if (slotString.equalsIgnoreCase("all")) {
            for (EntityEquipmentSlot slot : EntityEquipmentSlot.values()) {
                safelyApplyAttribute(itemStack, slot, attributeName, amount, operation);
            }
        } else {
            EntityEquipmentSlot slot = getEquipmentSlotByName(slotString);
            if (slot == null) {
                throw new CommandException("commands.extracmds.attribute.invalidSlot");
            }
            safelyApplyAttribute(itemStack, slot, attributeName, amount, operation);
        }

        if (server.sendCommandFeedback()) {
            ITextComponent textComponent = new TextComponentTranslation("commands.extracmds.attribute.success", attributeName, amount);
            textComponent.getStyle().setColor(TextFormatting.GREEN);
            sender.sendMessage(textComponent);
        }

        player.inventory.markDirty();
    }

    private void safelyApplyAttribute(ItemStack itemStack, EntityEquipmentSlot slot, String attributeName, double amount, int operation) {
        removeAttribute(itemStack, attributeName, slot);
        UUID slotUUID = getSlotSpecificUUID(attributeName, slot);
        AttributeModifier modifier = new AttributeModifier(slotUUID, attributeName, amount, operation);
        itemStack.addAttributeModifier(attributeName, modifier, slot);
    }

    private UUID getSlotSpecificUUID(String attributeName, EntityEquipmentSlot slot) {
        return UUID.nameUUIDFromBytes((attributeName + "_" + slot.getName()).getBytes());
    }

    private void removeAttribute(ItemStack itemStack, String attributeName, EntityEquipmentSlot slot) {
        if (!itemStack.hasTagCompound()) return;
        if (!itemStack.getTagCompound().hasKey("AttributeModifiers", 9)) return;

        NBTTagList list = itemStack.getTagCompound().getTagList("AttributeModifiers", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            if (tag.getString("AttributeName").equals(attributeName) && tag.getString("Slot").equalsIgnoreCase(slot.getName())) {
                list.removeTag(i);
                i--;
            }
        }
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
        } else if (args.length == 4) {
            return getListOfStringsMatchingLastWord(args, Arrays.asList("addition", "multiply_base", "multiply_total"));
        }
        return Collections.emptyList();
    }
}
