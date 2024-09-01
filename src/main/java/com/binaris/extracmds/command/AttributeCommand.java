package com.binaris.extracmds.command;

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

        // Custom WizardryUtil attributes
        attributeUUIDs.put("wizardryutils.SpellPotency", UUID.fromString("1F03D5C4-38F3-4A1E-B7B1-0B9B08C6D7AA"));
        attributeUUIDs.put("wizardryutils.SpellChargeup", UUID.fromString("2A67BB5C-19C1-4506-8157-9B3B5D8D8E1B"));
        attributeUUIDs.put("wizardryutils.SpellCost", UUID.fromString("3405C5BA-7A89-42B2-BF51-2FC8C9A1EB0F"));
        attributeUUIDs.put("wizardryutils.SpellDuration", UUID.fromString("4E909A2F-F319-4B3A-9A25-59A925A82D65"));
        attributeUUIDs.put("wizardryutils.SpellBlast", UUID.fromString("56B9F8A2-3999-4E85-8716-8A0D02F776E8"));
        attributeUUIDs.put("wizardryutils.SpellRange", UUID.fromString("64E963F7-3B87-4A65-9615-4E4A7F8A5F9B"));
        attributeUUIDs.put("wizardryutils.SpellCooldown", UUID.fromString("7C2F5B89-5B2B-4F31-A57A-7F3B8A8D9A9F"));
        attributeUUIDs.put("wizardryutils.SpellCondensing", UUID.fromString("84AEBEA8-75D2-42B7-889D-7A3B0B9EBA8C"));

        // Elemental attributes (Fire)
        attributeUUIDs.put("wizardryutils.FireSpellPotency", UUID.fromString("9DB22F42-5A89-4387-BF58-9C2F9B3A2B1E"));
        attributeUUIDs.put("wizardryutils.FireSpellCost", UUID.fromString("A81EB33D-62C9-4B75-9B41-3F9B2C8F3E9A"));
        attributeUUIDs.put("wizardryutils.FireSpellDuration", UUID.fromString("B39E7C45-7497-4B81-8D32-4B1D9C3E7B1F"));
        attributeUUIDs.put("wizardryutils.FireSpellBlast", UUID.fromString("C5029F6C-7C8A-4F89-8A5B-6C4A9B2F3A5E"));
        attributeUUIDs.put("wizardryutils.FireSpellRange", UUID.fromString("D6651C55-8B7A-4C8D-9781-7F3A8D9B5A6C"));
        attributeUUIDs.put("wizardryutils.FireSpellCooldown", UUID.fromString("E7D42B38-9A2B-4F87-AB89-8A2D3B4C5A7F"));

        // Elemental attributes (Ice)
        attributeUUIDs.put("wizardryutils.IceSpellPotency", UUID.fromString("F1C9A27D-A7C1-4A8F-AC3E-9C1A2B3F4D6E"));
        attributeUUIDs.put("wizardryutils.IceSpellCost", UUID.fromString("05692C81-9A5C-4B7F-8B2C-3A2D9C5F6A7E"));
        attributeUUIDs.put("wizardryutils.IceSpellDuration", UUID.fromString("16D78C94-AB1E-4C89-8A3B-5C6F9A1B7E2D"));
        attributeUUIDs.put("wizardryutils.IceSpellBlast", UUID.fromString("27841C2F-B7D2-4E8B-9723-7A5C8B9E4D2F"));
        attributeUUIDs.put("wizardryutils.IceSpellRange", UUID.fromString("3F7A5C8A-CB8F-4A7E-8972-6B4A9C2E7D8F"));
        attributeUUIDs.put("wizardryutils.IceSpellCooldown", UUID.fromString("489D2C1F-D1E2-4A9C-8721-7C3A8B5D9A6F"));

        // Elemental attributes (Lightning)
        attributeUUIDs.put("wizardryutils.LightningSpellPotency", UUID.fromString("59D4C28F-E7C2-4A7F-9A5D-8B2A7C5E9D7F"));
        attributeUUIDs.put("wizardryutils.LightningSpellCost", UUID.fromString("61A8B9F3-5C7D-4E89-8725-8A2C3D5E7A9F"));
        attributeUUIDs.put("wizardryutils.LightningSpellDuration", UUID.fromString("73D7A58C-6B4E-4A9C-8B7A-9A5D3C2F6A7E"));
        attributeUUIDs.put("wizardryutils.LightningSpellBlast", UUID.fromString("847A5D2B-7E8C-4A8F-9723-8A2D3C5E7A9F"));
        attributeUUIDs.put("wizardryutils.LightningSpellRange", UUID.fromString("957C8A2F-8D4A-4C7E-9A5D-9C3A2B5F7E8D"));
        attributeUUIDs.put("wizardryutils.LightningSpellCooldown", UUID.fromString("A67D2C4F-9A7E-4B8A-8725-9D3B7A5E8C9F"));

        // Elemental attributes (Necromancy)
        attributeUUIDs.put("wizardryutils.NecromancySpellPotency", UUID.fromString("B78D3A6F-9C7A-4A8B-8A5D-9C3A2D5E7F9A"));
        attributeUUIDs.put("wizardryutils.NecromancySpellCost", UUID.fromString("C89E2F4A-7C9B-4A8F-9723-8A2D3B5E6A7C"));
        attributeUUIDs.put("wizardryutils.NecromancySpellDuration", UUID.fromString("D89F3B7A-8A2C-4A9D-8A5B-9C2D3B5F6A7E"));
        attributeUUIDs.put("wizardryutils.NecromancySpellBlast", UUID.fromString("E9A24D7C-9B8F-4A8A-8725-8A2C3D5E6A7F"));
        attributeUUIDs.put("wizardryutils.NecromancySpellRange", UUID.fromString("FA5C2B8A-9E7C-4A8F-9A5B-7C3A2D5E8A9F"));
        attributeUUIDs.put("wizardryutils.NecromancySpellCooldown", UUID.fromString("0B6D2F9A-A7C8-4A9F-8A5D-7C3A2D5F6A7E"));

        // Elemental attributes (Earth)
        attributeUUIDs.put("wizardryutils.EarthSpellPotency", UUID.fromString("1B7C4A8F-B7D2-4A9F-8B5D-8A2C3D5E7F9A"));
        attributeUUIDs.put("wizardryutils.EarthSpellCost", UUID.fromString("2B8F3A7C-8C9B-4A9F-9723-8B2D3C5E6A7F"));
        attributeUUIDs.put("wizardryutils.EarthSpellDuration", UUID.fromString("3B9C4A8F-9A2B-4A9D-8A5B-9C2D3B5E7A8F"));
        attributeUUIDs.put("wizardryutils.EarthSpellBlast", UUID.fromString("4B7A5C8F-8A2C-4A9D-9A5B-7C3A2D5E6A9F"));
        attributeUUIDs.put("wizardryutils.EarthSpellRange", UUID.fromString("5B9E4C8F-9C7A-4A9F-8A5D-8C3A2B5F7A9E"));
        attributeUUIDs.put("wizardryutils.EarthSpellCooldown", UUID.fromString("6B8F3A9C-8A2C-4A9D-9A5B-7C3D2E5F6A8F"));

        // Sorcery attributes
        attributeUUIDs.put("wizardryutils.SorcerySpellPotency", UUID.fromString("D1E7A2B8-4A9F-8C9D-7B2A-9E5F8C7B1D2A"));
        attributeUUIDs.put("wizardryutils.SorcerySpellCost", UUID.fromString("E2C8A3D9-9B4A-8F7D-9A2C-7F3B6D5E1A7C"));
        attributeUUIDs.put("wizardryutils.SorcerySpellDuration", UUID.fromString("F3B7C9A2-7A8F-9B2D-8C5A-9D2B3F4A6E7C"));
        attributeUUIDs.put("wizardryutils.SorcerySpellBlast", UUID.fromString("0489C7E2-9A5D-8B2C-7F3A-8D4E7B6C1A9F"));
        attributeUUIDs.put("wizardryutils.SorcerySpellRange", UUID.fromString("159A8B7C-4A9D-7E8C-9B5F-8A2D3C6E7F1A"));
        attributeUUIDs.put("wizardryutils.SorcerySpellCooldown", UUID.fromString("26C8B7A9-7A9D-4F8E-8A2C-9B3A2E5F7C6D"));

        // Healing attributes
        attributeUUIDs.put("wizardryutils.HealingSpellPotency", UUID.fromString("37B8A7D2-9A4F-7C8B-9E5D-8C2A3F5B7D6E"));
        attributeUUIDs.put("wizardryutils.HealingSpellCost", UUID.fromString("48C9B8A7-7E4F-9A2D-8B3C-6A2D5F7C1E9B"));
        attributeUUIDs.put("wizardryutils.HealingSpellDuration", UUID.fromString("59E7A8B2-9C4D-7F8A-8D2B-9B3A7C6E5F1A"));
        attributeUUIDs.put("wizardryutils.HealingSpellBlast", UUID.fromString("6A9B8C7D-7A4F-9E5C-8B2D-7C3A5E6F9D1A"));
        attributeUUIDs.put("wizardryutils.HealingSpellRange", UUID.fromString("7B8C9A7F-4D9E-7C2A-8B5D-6A2E7F5B3C1D"));
        attributeUUIDs.put("wizardryutils.HealingSpellCooldown", UUID.fromString("8C9A7B6F-7D4E-9F2C-8A5D-7B3A6C1E5F7A"));
    }

    @Override
    public String getName() {
        return "attribute";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/attribute <slot> <attribute> <amount>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length != 3) {
            throw new CommandException("Invalid command usage. Usage: " + getUsage(sender));
        }

        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        ItemStack itemStack = player.getHeldItemMainhand();

        if (itemStack.isEmpty()) {
            throw new CommandException("You must be holding an item to use this command.");
        }

        EntityEquipmentSlot slot = getEquipmentSlotByName(args[0]);
        if (slot == null) {
            throw new CommandException("Invalid slot name.");
        }

        String attributeName = args[1];
        UUID attributeUUID = attributeUUIDs.get(attributeName);
        if (attributeUUID == null) {
            throw new CommandException("Invalid attribute name.");
        }

        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            throw new CommandException("Invalid amount format.");
        }


        for(Map.Entry<String, AttributeModifier> modifier : itemStack.getAttributeModifiers(slot).entries()) {
            if(modifier.getValue().getID().equals(attributeUUID)) {
                itemStack.getAttributeModifiers(slot).removeAll(attributeName);
            }
        }

        // Create or update the attribute modifier
        AttributeModifier modifier = new AttributeModifier(attributeUUID, attributeName, amount, 0);
        secureAddAttribute(itemStack, slot, modifier, attributeName);

        if (server.sendCommandFeedback()) {
            ITextComponent textComponent = new TextComponentString("Attribute " + attributeName + " added with amount " + amount);
            textComponent.getStyle().setColor(TextFormatting.GREEN);
            sender.sendMessage(textComponent);
        }
    }

    private static void secureAddAttribute(ItemStack itemStack, EntityEquipmentSlot slot, AttributeModifier modifier, String attributeName) {
        itemStack.addAttributeModifier(attributeName, modifier, slot);

        if (!itemStack.getTagCompound().hasKey("AttributeModifiers", 9)) {
            itemStack.getTagCompound().setTag("AttributeModifiers", new NBTTagList());
        }

        NBTTagList nbttaglist = itemStack.getTagCompound().getTagList("AttributeModifiers", 10);
        NBTTagCompound nbttagcompound = SharedMonsterAttributes.writeAttributeModifierToNBT(modifier);
        nbttagcompound.setString("AttributeName", attributeName);

        if (slot != null)
        {
            nbttagcompound.setString("Slot", slot.getName());
        }

        for(int i = 0; i < nbttaglist.tagCount(); ++i) {
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
            return getListOfStringsMatchingLastWord(args, Arrays.asList(EntityEquipmentSlot.values()));
        } else if (args.length == 2) {
            return getListOfStringsMatchingLastWord(args, attributeUUIDs.keySet());
        } else {
            return Collections.emptyList();
        }
    }
}
