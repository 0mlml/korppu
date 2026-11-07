package dev.mlml.korppu.mixin;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.event.events.ChunkDataEvent;
import dev.mlml.korppu.event.events.EntitySpawnEvent;
import dev.mlml.korppu.event.events.PlayedSoundEvent;
import dev.mlml.korppu.event.events.PlayerRespawnEvent;
import dev.mlml.korppu.event.events.PlayerTrackEvent;
import dev.mlml.korppu.event.events.TeleportParticleEvent;
import dev.mlml.korppu.misc.SeedState;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ClientPacketListener.class)
public abstract class ClientPlayNetworkHandlerMixin {
    @Inject(method = "handleLevelChunkWithLight", at = @At("TAIL"))
    private void onChunkData(ClientboundLevelChunkWithLightPacket packet, CallbackInfo info) {
        LevelChunk chunk = KorppuMod.mc.level.getChunk(packet.getX(), packet.getZ());
        KorppuMod.eventManager.trigger(new ChunkDataEvent(chunk));
    }

    @Inject(method = "handleLogin", at = @At("TAIL"))
    private void onGameJoin(ClientboundLoginPacket packet, CallbackInfo ci) {
        SeedState.setSeed(extractSeed(packet), "join");
    }

    @Inject(method = "handleAddEntity",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/ClientLevel;addEntity(Lnet/minecraft/world/entity/Entity;)V",
                    shift = At.Shift.AFTER),
            locals = LocalCapture.CAPTURE_FAILHARD)
    private void onEntitySpawn(ClientboundAddEntityPacket packet, CallbackInfo ci, Entity entity) {
        KorppuMod.eventManager.trigger(new EntitySpawnEvent(entity));

        if (entity instanceof Player player && entity != KorppuMod.mc.player) {
            KorppuMod.eventManager.trigger(
                    new PlayerTrackEvent.Enter(player.getDisplayName().getString(), player.getUUID()));
        }
    }

    @Inject(method = "handleRemoveEntities", at = @At("HEAD"))
    private void onEntitiesDestroy(ClientboundRemoveEntitiesPacket packet, CallbackInfo ci) {
        if (KorppuMod.mc.level == null) return;
        for (int entityId : packet.getEntityIds()) {
            Entity entity = KorppuMod.mc.level.getEntity(entityId);
            if (entity instanceof Player player && entity != KorppuMod.mc.player) {
                KorppuMod.eventManager.trigger(
                        new PlayerTrackEvent.Leave(player.getDisplayName().getString(), player.getUUID()));
            }
        }
    }

    @Inject(method = "handleRespawn", at = @At("HEAD"))
    private void onPlayerRespawn(ClientboundRespawnPacket packet, CallbackInfo ci) {
        KorppuMod.eventManager.trigger(new PlayerRespawnEvent());
    }

    @Inject(method = "handleSoundEvent", at = @At("TAIL"))
    private void onPlaySound(ClientboundSoundPacket packet, CallbackInfo ci) {
        Vec3 pos = new Vec3(packet.getX(), packet.getY(), packet.getZ());
        String soundId = BuiltInRegistries.SOUND_EVENT.getKey(packet.getSound().value()).toString();
        KorppuMod.eventManager.trigger(new PlayedSoundEvent(pos, soundId));
    }

    @Inject(method = "handleParticleEvent", at = @At("TAIL"))
    private void onParticle(ClientboundLevelParticlesPacket packet, CallbackInfo ci) {
        String particleId = BuiltInRegistries.PARTICLE_TYPE.getKey(packet.getParticle().getType()).toString();
        if (particleId.contains("portal") || particleId.contains("ender")) {
            KorppuMod.eventManager.trigger(new TeleportParticleEvent(new Vec3(packet.getX(), packet.getY(), packet.getZ()), particleId));
        }
    }

    private static Long extractSeed(ClientboundLoginPacket packet) {
        try {
            for (var field : packet.getClass().getDeclaredFields()) {
                if (field.getType() == long.class && field.getName().toLowerCase().contains("seed")) {
                    field.setAccessible(true);
                    return field.getLong(packet);
                }
            }
        } catch (Throwable ignored) {
        }

        return null;
    }
}
