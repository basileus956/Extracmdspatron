package com.binaris.extracmds;

import net.minecraft.item.ItemArmorStand;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@EventBusSubscriber(modid = ExtraCMDS.MODID)
public class EventHandlerCommand {

    @SubscribeEvent
    public void onArmorStand(PlayerInteractEvent.RightClickBlock event) {
        ItemStack itemStack = event.getItemStack();

        if(itemStack.getItem() instanceof ItemArmorStand){
            CommandUtil.summonArmorStand(event);
            if(!event.getEntityPlayer().isCreative()) itemStack.shrink(1);

        }
    }
}
