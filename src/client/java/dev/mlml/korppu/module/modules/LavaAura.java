package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.mixin.IPlayerInventoryMixin;
import dev.mlml.korppu.module.EntityTargetSelector;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.StatusEntry;
import dev.mlml.korppu.player.PlayerManager;
import dev.mlml.korppu.player.PlayerRelation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Set;

public class LavaAura extends Module {
    private final DoubleSetting range = config.add(new DoubleSetting("Range", "Maximum target range", 5d, 1d, 12d, 0));
    private final DoubleSetting delayTicks = config.add(new DoubleSetting("Delay", "Ticks between placements", 5d, 0d, 40d, 0));
    private final EntityTargetSelector targets = new EntityTargetSelector(config, "Targets", Set.of("PLAYERS"));
    private final BooleanSetting enemiesOnly = config.add(new BooleanSetting("Enemies Only", "Only place near enemies", true));

    private int cooldown;
    private int placements;

    public LavaAura() {
        super("LavaAura", "Places lava near nearby targets", GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onDisable() {
        cooldown = 0;
    }

    @Override
    public void onTick() {
        if (KorppuMod.mc.player == null || KorppuMod.mc.level == null || KorppuMod.mc.gameMode == null) return;
        if (cooldown > 0) {
            cooldown--;
            return;
        }

        int lavaSlot = findLavaSlot();
        if (lavaSlot < 0) return;

        for (var player : KorppuMod.mc.level.players()) {
            if (player == KorppuMod.mc.player) continue;
            if (!targets.matches(player)) continue;
            if (KorppuMod.mc.player.distanceToSqr(player) > range.getValue() * range.getValue()) continue;

            if (enemiesOnly.getValue()) {
                PlayerRelation rel = PlayerManager.getInstance().getRelation(player.getUUID());
                if (rel != PlayerRelation.ENEMY) continue;
            }

            BlockPos feet = player.blockPosition();
            BlockPos place = feet.above();
            if (!KorppuMod.mc.level.getBlockState(place).canBeReplaced()) continue;

            if (placeLava(lavaSlot, feet, place)) {
                placements++;
                cooldown = delayTicks.getValue().intValue();
                return;
            }
        }
    }

    private boolean placeLava(int slot, BlockPos support, BlockPos hitPos) {
        IPlayerInventoryMixin inv = (IPlayerInventoryMixin) (Object) KorppuMod.mc.player.getInventory();
        int old = inv.getSelectedSlot();
        inv.setSelectedSlot(slot);

        BlockHitResult bhr = new BlockHitResult(hitPos.getCenter(), Direction.UP, support, false);
        var result = KorppuMod.mc.gameMode.useItemOn(KorppuMod.mc.player, InteractionHand.MAIN_HAND, bhr);
        KorppuMod.mc.player.swing(InteractionHand.MAIN_HAND);

        inv.setSelectedSlot(old);
        return result != null && result.consumesAction();
    }

    private int findLavaSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = KorppuMod.mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() == Items.LAVA_BUCKET) return i;
        }
        return -1;
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        return List.of(
                StatusEntry.text("Placed", String.valueOf(placements)),
                StatusEntry.text("Cooldown", String.valueOf(cooldown)),
                StatusEntry.text("Targets", targets.summary()),
                StatusEntry.indicator("Enemies Only", enemiesOnly.getValue())
        );
    }
}
