package com.binaris.extracmds;

import com.binaris.extracmds.command.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import org.apache.logging.log4j.Logger;

@Mod(modid = ExtraCMDS.MODID, name = ExtraCMDS.NAME, version = ExtraCMDS.VERSION)
public class ExtraCMDS {
    public static final String MODID = "extracmds";
    public static final String NAME = "ExtraCMDS";
    public static final String VERSION = "1.0.0";

    public static final boolean WIZARDRYUTILS_LOADED = Loader.isModLoaded("wizardryutils");

    public static Logger logger;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new EventHandlerCommand());

//        // some example code
//        logger.info("DIRT BLOCK >> {}", Blocks.DIRT.getRegistryName());
    }

    @EventHandler
    public void serverStarting(FMLServerStartingEvent event){
        event.registerServerCommand(new FixCommand());
        event.registerServerCommand(new SkullCommand());
        event.registerServerCommand(new RenameCommand());
        event.registerServerCommand(new ReplaceLoreLineCommand());
        event.registerServerCommand(new LoreCommand());
        event.registerServerCommand(new EnchantifyCommand());
        event.registerServerCommand(new AttributeCommand());
        event.registerServerCommand(new PotionCommand());
        event.registerServerCommand(new HealCommand());
        event.registerServerCommand(new FeedCommand());
        event.registerServerCommand(new UnenchantCommand());
        event.registerServerCommand(new RemoveLoreCommand());
        event.registerServerCommand(new RemoveAttributeCommand());
        event.registerServerCommand(new UnbreakableCommand());
        event.registerServerCommand(new SoarCommand());
        event.registerServerCommand(new BookCommand());
        event.registerServerCommand(new GlowCommand());
        event.registerServerCommand(new UnglowCommand());
        event.registerServerCommand(new PeekCommand());
        event.registerServerCommand(new XPBottleCommand());
        event.registerServerCommand(new TagRodCommand());
        event.registerServerCommand(new ImbueCommand());
    }
}

