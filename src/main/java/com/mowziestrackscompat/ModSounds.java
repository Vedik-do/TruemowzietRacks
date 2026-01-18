package com.mowziestrackscompat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MowziesTracksCompat.MODID);

    public static final RegistryObject<SoundEvent> FERROUS_WROUGHTNAUT_BOSS = register("ferrous_wroughtnaut_boss");
    public static final RegistryObject<SoundEvent> UMVUTHI_BOSS = register("umvuthi_boss");
    public static final RegistryObject<SoundEvent> FROSTMAW_BOSS = register("frostmaw_boss");
    public static final RegistryObject<SoundEvent> TONGBI_BOSS = register("tongbi_boss");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MowziesTracksCompat.MODID, name)));
    }

    public static void register(IEventBus bus) {
        SOUND_EVENTS.register(bus);
    }
}
