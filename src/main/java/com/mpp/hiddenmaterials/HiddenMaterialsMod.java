package com.mpp.hiddenmaterials;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

@Mod(modid = ModMeta.MOD_ID, name = ModMeta.NAME, version = ModMeta.VERSION,
     dependencies = "required-after:mixinbooter;required-after:crafttweaker;required-after:gamestages;required-after:tconstruct")
public class HiddenMaterialsMod
{
    public static final String MODID = ModMeta.MOD_ID;

    private static Logger logger;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();
        logger.info("Hidden Materials loaded: Tinkers book materials can be gated behind Game Stages "
                + "through mods.hiddenmaterials.MaterialStages");
    }

    public static Logger getLogger()
    {
        return logger;
    }
}
