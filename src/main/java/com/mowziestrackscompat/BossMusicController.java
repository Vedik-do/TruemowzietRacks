package com.mowziestrackscompat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = MowziesTracksCompat.MODID, value = Dist.CLIENT)
public class BossMusicController {

    // Boss entity IDs (Mowzie's Mobs)
    private static final ResourceLocation ID_FERROUS = new ResourceLocation("mowziesmobs", "ferrous_wroughtnaut");
    private static final ResourceLocation ID_UMVUTHI = new ResourceLocation("mowziesmobs", "umvuthi");
    private static final ResourceLocation ID_FROSTMAW = new ResourceLocation("mowziesmobs", "frostmaw");
    private static final ResourceLocation ID_TONGBI = new ResourceLocation("mowziesmobs", "tongbi");

    // detection radius
    private static final double RADIUS = 80.0;
    // how long (ticks) we keep boss mode after last seen (prevents flicker)
    private static final int GRACE_TICKS = 40;

    private static SoundInstance current;
    private static ResourceLocation currentBossId;
    private static int lastSeenTicks = 99999;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Player player = mc.player;
        ClientLevel level = mc.level;

        ResourceLocation boss = findBossNear(level, player);

        if (boss != null) {
            lastSeenTicks = 0;
            // Kill background music (vanilla or mods using the music channel)
            stopVanillaMusic(mc);
            stopOverhauledMusicIfPresent();

            if (!boss.equals(currentBossId)) {
                stopCurrent(mc);
                playBoss(mc, boss);
                currentBossId = boss;
            } else {
                // ensure it keeps playing; if it stopped for any reason, restart
                if (current != null && !mc.getSoundManager().isActive(current)) {
                    playBoss(mc, boss);
                }
            }
        } else {
            lastSeenTicks++;
            if (current != null && lastSeenTicks > GRACE_TICKS) {
                stopCurrent(mc);
                currentBossId = null;
            }
        }
    }

    // Optional: prevent new MUSIC sounds from starting during boss mode
    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        if (current != null && lastSeenTicks <= GRACE_TICKS) {
            if (event.getSound() != null && event.getSound().getSource() == SoundSource.MUSIC) {
                // Allow our own sound, block everything else
                ResourceLocation loc = event.getSound().getLocation();
                if (loc == null || !MowziesTracksCompat.MODID.equals(loc.getNamespace())) {
                    event.setCanceled(true);
                }
            }
        }
    }

    private static void playBoss(Minecraft mc, ResourceLocation bossId) {
        SoundEvent se;
        if (bossId.equals(ID_FERROUS)) se = ModSounds.FERROUS_WROUGHTNAUT_BOSS.get();
        else if (bossId.equals(ID_UMVUTHI)) se = ModSounds.UMVUTHI_BOSS.get();
        else if (bossId.equals(ID_FROSTMAW)) se = ModSounds.FROSTMAW_BOSS.get();
        else se = ModSounds.TONGBI_BOSS.get();

        LoopingMusic inst = new LoopingMusic(se);
        current = inst;
        mc.getSoundManager().play(inst);
    }

    private static void stopCurrent(Minecraft mc) {
        if (current != null) {
            mc.getSoundManager().stop(current);
            current = null;
        }
    }

    private static void stopVanillaMusic(Minecraft mc) {
        try {
            mc.getMusicManager().stopPlaying();
        } catch (Throwable ignored) {
        }
    }

    private static ResourceLocation findBossNear(ClientLevel level, Player player) {
        AABB box = player.getBoundingBox().inflate(RADIUS);
        List<Entity> ents = level.getEntities(player, box, e -> e != null && e.isAlive());
        for (Entity e : ents) {
            ResourceLocation typeId = ForgeRegistries.ENTITY_TYPES.getKey(e.getType());
            if (typeId == null) continue;
            // priority order
            if (typeId.equals(ID_FERROUS)) return ID_FERROUS;
            if (typeId.equals(ID_UMVUTHI)) return ID_UMVUTHI;
            if (typeId.equals(ID_FROSTMAW)) return ID_FROSTMAW;
            if (typeId.equals(ID_TONGBI)) return ID_TONGBI;
        }
        return null;
    }

    // Reflection: stop OverhauledMusic currently playing tracks (if installed)
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void stopOverhauledMusicIfPresent() {
        try {
            Class<?> clientEvents = Class.forName("com.overhauledmusic.client.ClientEvents");
            Field directorField = clientEvents.getDeclaredField("DIRECTOR");
            directorField.setAccessible(true);
            Object director = directorField.get(null);
            if (director == null) return;

            Field instancesField = director.getClass().getDeclaredField("instances");
            instancesField.setAccessible(true);
            Object mapObj = instancesField.get(director);
            if (!(mapObj instanceof Map)) return;
            Map map = (Map) mapObj;

            for (Object inst : map.values()) {
                if (inst == null) continue;
                // FadingMusicInstance has stopSound()
                Method stopSound = inst.getClass().getDeclaredMethod("stopSound");
                stopSound.setAccessible(true);
                stopSound.invoke(inst);
            }

            // also clear current
            Field currentField = director.getClass().getDeclaredField("current");
            currentField.setAccessible(true);
            currentField.set(director, null);
        } catch (Throwable ignored) {
            // OverhauledMusic not installed or changed; ignore.
        }
    }

    // Looping music instance in MUSIC channel
    private static class LoopingMusic extends AbstractSoundInstance {
        protected LoopingMusic(SoundEvent sound) {
            super(sound, SoundSource.MUSIC, SoundInstance.createUnseededRandom());
            this.looping = true;
            this.delay = 0;
            this.volume = 1.0f;
            this.pitch = 1.0f;
            this.relative = true;
            this.x = 0.0;
            this.y = 0.0;
            this.z = 0.0;
        }

        @Override
        public boolean canPlaySound() {
            return true;
        }
    }
}
