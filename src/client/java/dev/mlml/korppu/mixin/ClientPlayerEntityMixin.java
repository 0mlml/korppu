package dev.mlml.korppu.mixin;

import com.mojang.authlib.GameProfile;
import dev.mlml.korppu.module.ModuleManager;
import dev.mlml.korppu.module.modules.Passives;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin extends AbstractClientPlayerEntity {
    public ClientPlayerEntityMixin(ClientWorld world,
                                   GameProfile profile) {
        super(world, profile);
    }

    @Override
    public double getBlockInteractionRange() {
        Passives passives = (Passives) ModuleManager.getModule(Passives.class);
        if (passives != null && passives.isEnabled() && passives.getReach().getValue()) {
            return passives.getBlockReachDistance().getValue();
        }
        return super.getBlockInteractionRange();
    }

    @Override
    public double getEntityInteractionRange() {
        Passives passives = ModuleManager.getModule(Passives.class);
        if (passives != null && passives.isEnabled() && passives.getReach().getValue()) {
            return passives.getEntityReachDistance().getValue();
        }
        return super.getEntityInteractionRange();
    }
}
