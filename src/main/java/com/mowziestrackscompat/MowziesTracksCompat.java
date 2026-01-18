package com.mowziestrackscompat;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(MowziesTracksCompat.MODID)
public class MowziesTracksCompat {
    public static final String MODID = "mowziestrackscompat";

    public MowziesTracksCompat() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModSounds.register(modBus);
    }
}
