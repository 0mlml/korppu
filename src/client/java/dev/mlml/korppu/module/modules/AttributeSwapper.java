package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.event.Listener;
import dev.mlml.korppu.event.events.PacketEvent;
import dev.mlml.korppu.imgui.windows.AttributeSwapperControlWindow;
import dev.mlml.korppu.imgui.windows.ModuleControlWindow;
import dev.mlml.korppu.mixin.IPlayerInventoryMixin;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.NotifierApi;
import dev.mlml.korppu.module.StatusEntry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class AttributeSwapper extends Module {
    private final DoubleSetting scanRange = config.add(
            new DoubleSetting("Scan Range", "How far to search for a target when not looking at one", 8d, 2d, 16d, 1));
    private final DoubleSetting spearReachBonus = config.add(
            new DoubleSetting("Spear Reach Bonus", "Estimated extra reach from spear in swap chain", 1.5d, 0d, 4d, 1));
    private final DoubleSetting unknownArmor = config.add(
            new DoubleSetting("Unknown Armor", "Assumed armor points when target armor is unknown", 10d, 0d, 30d, 0));
    private final BooleanSetting considerSpear = config.add(
            new BooleanSetting("Use Spear Reach", "Use spear as second step when outside normal reach", true));
    private final BooleanSetting considerBreach = config.add(
            new BooleanSetting("Use Mace Breach", "Account for breach on mace in second step", true));
    private final BooleanSetting notifyOnChainChange = config.add(
            new BooleanSetting("Notify Chain", "Show notifier when best chain changes", false));
    private final BooleanSetting requireFullCooldown = config.add(
            new BooleanSetting("Require Full Cooldown", "Only perform swap chain on fully charged attacks", true));
    private final BooleanSetting strictPriming = config.add(
            new BooleanSetting("Strict Priming", "Only chain when currently holding first weapon", true));

    private Plan plan = Plan.empty();
    private String lastNotifiedKey = "";
    private boolean injectingSwap;
    private int restoreSlot = -1;
    private int localRestoreSlot = -1;
    private int lockedFirstSlot = -1;
    private int lockedSecondSlot = -1;

    public AttributeSwapper() {
        super("AttributeSwapper", "Suggests optimal attribute-swap chain from hotbar", GLFW.GLFW_KEY_UNKNOWN);
        KorppuMod.eventManager.register(this);
    }

    @Override
    public void onTick() {
        if (KorppuMod.mc.player == null || KorppuMod.mc.level == null) {
            plan = Plan.empty();
            restoreSlot = -1;
            localRestoreSlot = -1;
            return;
        }

        if (restoreSlot >= 0 && KorppuMod.mc.getConnection() != null) {
            KorppuMod.mc.getConnection().send(new ServerboundSetCarriedItemPacket(restoreSlot));
            if (localRestoreSlot >= 0) {
                ((IPlayerInventoryMixin) (Object) KorppuMod.mc.player.getInventory()).setSelectedSlot(localRestoreSlot);
            }
            restoreSlot = -1;
            localRestoreSlot = -1;
        }

        LivingEntity target = resolveTarget();
        plan = buildPlan(target);

        if (notifyOnChainChange.getValue() && plan.valid()) {
            String key = plan.chainString() + "|" + Math.round(plan.estimatedDamage() * 10);
            if (!key.equals(lastNotifiedKey)) {
                lastNotifiedKey = key;
                NotifierApi.push("AttributeSwapper", "Best chain: " + plan.chainString());
            }
        }
    }

    private LivingEntity resolveTarget() {
        if (KorppuMod.mc.hitResult != null && KorppuMod.mc.hitResult.getType() == HitResult.Type.ENTITY) {
            Entity entity = ((EntityHitResult) KorppuMod.mc.hitResult).getEntity();
            if (entity instanceof LivingEntity living && living.isAlive()) {
                return living;
            }
        }

        double maxDist = scanRange.getValue();
        double best = Double.MAX_VALUE;
        LivingEntity bestEntity = null;
        for (Entity e : KorppuMod.mc.level.entitiesForRendering()) {
            if (!(e instanceof LivingEntity living)) continue;
            if (living == KorppuMod.mc.player || !living.isAlive()) continue;
            double dist = KorppuMod.mc.player.distanceTo(living);
            if (dist <= maxDist && dist < best) {
                best = dist;
                bestEntity = living;
            }
        }
        return bestEntity;
    }

    private Plan buildPlan(LivingEntity target) {
        if (KorppuMod.mc.player == null) return Plan.empty();

        HotbarItem first = lockedFirstSlot >= 0 ? getHotbarItem(lockedFirstSlot, target) : bestBaseWeapon();
        if (first == null) {
            return new Plan(target, null, null, 0, 0, false,
                    List.of("No weapon in hotbar. Add a sword/axe/mace/spear."));
        }

        double baseReach = KorppuMod.mc.player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
        double distance = target != null ? KorppuMod.mc.player.distanceTo(target) : 0;
        HotbarItem second = lockedSecondSlot >= 0
                ? getHotbarItem(lockedSecondSlot, target)
                : bestSecondStep(target, baseReach, distance);
        if (second == null) second = first;

        double effectiveReach = baseReach + (second.stack().is(ItemTags.SPEARS) && considerSpear.getValue()
                ? spearReachBonus.getValue()
                : 0);

        double armorPoints = target != null ? target.getArmorValue() : unknownArmor.getValue();
        double dmg = first.baseDamage() + second.enchantDamage() + breachBonus(second.stack(), armorPoints);
        boolean inReach = target == null || distance <= effectiveReach + 0.1;

        List<String> suggestions = buildSuggestions(target, first, bestSpear(), second, baseReach, effectiveReach, distance);
        return new Plan(target, first, second, effectiveReach, dmg, inReach, suggestions);
    }

    private List<String> buildSuggestions(LivingEntity target, HotbarItem first, HotbarItem spear, HotbarItem second,
                                          double baseReach, double effectiveReach, double distance) {
        List<String> tips = new ArrayList<>();
        if (target == null) {
            tips.add("Look at a target to get distance-accurate chain.");
        }
        if (spear == null) {
            tips.add("Add a spear to hotbar for extra swap reach.");
        }
        if (target != null && distance > effectiveReach) {
            tips.add(String.format("Move %.1f blocks closer or use more reach.", distance - effectiveReach));
        }
        if (second != null && second.enchantDamage() < 1.0) {
            tips.add("Add Sharpness/Smite/Bane to second-step weapon.");
        }
        if (considerBreach.getValue() && (target == null || target.getArmorValue() > 8)) {
            int bestBreach = getBestBreachLevel();
            if (bestBreach == 0) {
                tips.add("A breach-enchanted mace can increase damage vs armor.");
            }
        }
        if (first.baseDamage() < 6.0) {
            tips.add("Upgrade first weapon base damage for stronger swaps.");
        }
        if (tips.isEmpty()) {
            tips.add("Chain already near-optimal for current target.");
        }
        return tips;
    }

    private int getBestBreachLevel() {
        if (KorppuMod.mc.player == null) return 0;
        int best = 0;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = KorppuMod.mc.player.getInventory().getItem(i);
            if (stack.isEmpty() || !stack.is(Items.MACE)) continue;
            best = Math.max(best, getEnchantLevel(stack, Enchantments.BREACH));
        }
        return best;
    }

    private HotbarItem bestBaseWeapon() {
        if (KorppuMod.mc.player == null) return null;

        HotbarItem best = null;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = KorppuMod.mc.player.getInventory().getItem(i);
            if (stack.isEmpty() || !isWeaponCandidate(stack)) continue;
            HotbarItem h = new HotbarItem(i, stack, estimateBaseDamage(stack), estimateEnchantDamage(stack, null));
            if (best == null || h.baseDamage() > best.baseDamage()) {
                best = h;
            }
        }
        return best;
    }

    private HotbarItem getHotbarItem(int slot, LivingEntity target) {
        if (KorppuMod.mc.player == null || slot < 0 || slot > 8) return null;
        ItemStack stack = KorppuMod.mc.player.getInventory().getItem(slot);
        if (stack.isEmpty()) return null;
        double ench = estimateEnchantDamage(stack, target)
                + breachBonus(stack, target != null ? target.getArmorValue() : unknownArmor.getValue());
        return new HotbarItem(slot, stack, estimateBaseDamage(stack), ench);
    }

    private HotbarItem bestSpear() {
        if (KorppuMod.mc.player == null) return null;
        HotbarItem best = null;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = KorppuMod.mc.player.getInventory().getItem(i);
            if (stack.isEmpty() || !stack.is(ItemTags.SPEARS)) continue;
            HotbarItem h = new HotbarItem(i, stack, estimateBaseDamage(stack), estimateEnchantDamage(stack, null));
            if (best == null || h.baseDamage() > best.baseDamage()) best = h;
        }
        return best;
    }

    private HotbarItem bestSecondStep(LivingEntity target, double baseReach, double distance) {
        if (KorppuMod.mc.player == null) return null;

        if (target instanceof Player playerTarget
                && playerTarget.isBlocking()
                && playerTarget.getActiveItem().is(Items.SHIELD)) {
            HotbarItem bestAxe = null;
            for (int i = 0; i < 9; i++) {
                ItemStack stack = KorppuMod.mc.player.getInventory().getItem(i);
                if (stack.isEmpty() || !(stack.getItem() instanceof AxeItem)) continue;
                double ench = estimateEnchantDamage(stack, target) + breachBonus(stack, target.getArmorValue());
                HotbarItem candidate = new HotbarItem(i, stack, estimateBaseDamage(stack), ench);
                if (bestAxe == null || candidate.enchantDamage() > bestAxe.enchantDamage()) {
                    bestAxe = candidate;
                }
            }
            if (bestAxe != null) return bestAxe;
        }

        HotbarItem bestReachValid = null;
        HotbarItem bestAny = null;
        double armorPoints = target != null ? target.getArmorValue() : unknownArmor.getValue();

        for (int i = 0; i < 9; i++) {
            ItemStack stack = KorppuMod.mc.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;

            double extraReach = stack.is(ItemTags.SPEARS) && considerSpear.getValue() ? spearReachBonus.getValue() : 0;
            double reach = baseReach + extraReach;
            boolean reachValid = target == null || distance <= reach + 0.1;

            double ench = estimateEnchantDamage(stack, target) + breachBonus(stack, armorPoints);
            HotbarItem candidate = new HotbarItem(i, stack, estimateBaseDamage(stack), ench);

            if (bestAny == null || candidate.enchantDamage() > bestAny.enchantDamage()) {
                bestAny = candidate;
            }
            if (reachValid && (bestReachValid == null || candidate.enchantDamage() > bestReachValid.enchantDamage())) {
                bestReachValid = candidate;
            }
        }

        return bestReachValid != null ? bestReachValid : bestAny;
    }

    private boolean isWeaponCandidate(ItemStack stack) {
        return stack.getItem() instanceof AxeItem
                || stack.is(ItemTags.SWORDS)
                || stack.is(ItemTags.SPEARS)
                || stack.is(Items.MACE)
                || stack.is(Items.TRIDENT);
    }

    private double breachBonus(ItemStack stack, double armorPoints) {
        if (!considerBreach.getValue() || !stack.is(Items.MACE)) return 0;
        int breach = getEnchantLevel(stack, Enchantments.BREACH);
        if (breach <= 0 || armorPoints <= 0) return 0;
        return armorPoints * (0.10 * breach);
    }

    private double estimateEnchantDamage(ItemStack stack, LivingEntity target) {
        int sharp = getEnchantLevel(stack, Enchantments.SHARPNESS);
        double bonus = sharp > 0 ? (0.5 * sharp + 0.5) : 0;

        if (target instanceof Monster) {
            int smite = getEnchantLevel(stack, Enchantments.SMITE);
            bonus += smite * 2.5;
        }
        int bane = getEnchantLevel(stack, Enchantments.BANE_OF_ARTHROPODS);
        if (target != null && bane > 0) bonus += bane;

        int fireAspect = getEnchantLevel(stack, Enchantments.FIRE_ASPECT);
        if (fireAspect > 0) bonus += fireAspect;

        return bonus;
    }

    private int getEnchantLevel(ItemStack stack, ResourceKey<net.minecraft.world.item.enchantment.Enchantment> key) {
        ItemEnchantments ench = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        final int[] level = {0};
        ench.entrySet().forEach(e -> {
            if (e.getKey().unwrapKey().isPresent() && e.getKey().unwrapKey().get().equals(key)) {
                level[0] = e.getIntValue();
            }
        });
        return level[0];
    }

    private double estimateBaseDamage(ItemStack stack) {
        if (stack.is(Items.NETHERITE_AXE)) return 10;
        if (stack.is(Items.DIAMOND_AXE)) return 9;
        if (stack.is(Items.IRON_AXE)) return 9;
        if (stack.is(Items.STONE_AXE)) return 9;
        if (stack.is(Items.GOLDEN_AXE)) return 7;
        if (stack.is(Items.WOODEN_AXE)) return 7;

        if (stack.is(Items.NETHERITE_SWORD)) return 8;
        if (stack.is(Items.DIAMOND_SWORD)) return 7;
        if (stack.is(Items.IRON_SWORD)) return 6;
        if (stack.is(Items.STONE_SWORD)) return 5;
        if (stack.is(Items.GOLDEN_SWORD)) return 4;
        if (stack.is(Items.WOODEN_SWORD)) return 4;

        if (stack.is(Items.MACE)) return 5;
        if (stack.is(Items.TRIDENT)) return 8;
        if (stack.is(ItemTags.SPEARS)) return 6;
        return 1;
    }

    @Override
    public String getStatus() {
        if (!plan.valid()) return "no chain";
        return plan.chainString();
    }

    @Listener
    public void onPacketSend(PacketEvent.Sent event) {
        if (!isEnabled() || injectingSwap || KorppuMod.mc.player == null || KorppuMod.mc.getConnection() == null) return;
        if (!(event.getPacket() instanceof ServerboundInteractPacket attackPacket)) return;
        if (!plan.valid()) return;
        if (requireFullCooldown.getValue() && KorppuMod.mc.player.getAttackStrengthScale(0.5f) < 1.0f) return;

        int originalSlot = ((IPlayerInventoryMixin) (Object) KorppuMod.mc.player.getInventory()).getSelectedSlot();
        if (strictPriming.getValue() && originalSlot != plan.first().slot()) return;

        injectingSwap = true;
        try {
            int b = plan.second().slot();

            sendSlotIfChanged(originalSlot, b);
            if (b != originalSlot) {
                restoreSlot = originalSlot;
                localRestoreSlot = originalSlot;
            }
        } finally {
            injectingSwap = false;
        }
    }

    private void sendSlotIfChanged(int from, int to) {
        if (to < 0 || to > 8) return;
        if (from == to) return;
        if (KorppuMod.mc.getConnection() == null) return;
        KorppuMod.mc.getConnection().send(new ServerboundSetCarriedItemPacket(to));
    }

    public void lockSelectedAsFirst() {
        if (KorppuMod.mc.player == null) return;
        int slot = ((IPlayerInventoryMixin) (Object) KorppuMod.mc.player.getInventory()).getSelectedSlot();
        ItemStack stack = KorppuMod.mc.player.getInventory().getItem(slot);
        if (!stack.isEmpty()) lockedFirstSlot = slot;
    }

    public void lockSelectedAsSecond() {
        if (KorppuMod.mc.player == null) return;
        int slot = ((IPlayerInventoryMixin) (Object) KorppuMod.mc.player.getInventory()).getSelectedSlot();
        ItemStack stack = KorppuMod.mc.player.getInventory().getItem(slot);
        if (!stack.isEmpty()) lockedSecondSlot = slot;
    }

    public void clearLocks() {
        lockedFirstSlot = -1;
        lockedSecondSlot = -1;
    }

    public String getLockedFirstLabel() {
        return lockedSlotLabel(lockedFirstSlot);
    }

    public String getLockedSecondLabel() {
        return lockedSlotLabel(lockedSecondSlot);
    }

    private String lockedSlotLabel(int slot) {
        if (KorppuMod.mc.player == null || slot < 0 || slot > 8) return "auto";
        ItemStack stack = KorppuMod.mc.player.getInventory().getItem(slot);
        if (stack.isEmpty()) return "auto";
        return "[" + (slot + 1) + "] " + stack.getHoverName().getString();
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        List<StatusEntry> entries = new ArrayList<>();
        entries.add(StatusEntry.text("Mode", "Optimizer"));

        if (plan.target() == null) {
            entries.add(StatusEntry.text("Target", "none"));
        } else {
            entries.add(StatusEntry.text("Target", plan.target().getDisplayName().getString()));
            entries.add(StatusEntry.text("Distance", String.format("%.2f", KorppuMod.mc.player.distanceTo(plan.target()))));
        }

        if (!plan.valid()) {
            entries.add(StatusEntry.text("Chain", "unavailable"));
        } else {
            entries.add(StatusEntry.text("Chain", plan.chainString()));
            entries.add(StatusEntry.text("First", plan.first().display()));
            entries.add(StatusEntry.text("Second", plan.second().display()));
            entries.add(StatusEntry.text("Reach", String.format("%.2f", plan.reach())));
            entries.add(StatusEntry.text("Damage Est", String.format("%.2f", plan.estimatedDamage())));
            entries.add(StatusEntry.indicator("In Reach", plan.inReach()));
        }

        entries.add(StatusEntry.text("Lock Slot1", getLockedFirstLabel()));
        entries.add(StatusEntry.text("Lock Slot2", getLockedSecondLabel()));

        int tips = Math.min(3, plan.suggestions().size());
        for (int i = 0; i < tips; i++) {
            entries.add(StatusEntry.text("Tip " + (i + 1), plan.suggestions().get(i), 0xFFFFFF55));
        }

        return entries;
    }

    private record HotbarItem(int slot, ItemStack stack, double baseDamage, double enchantDamage) {
        public String display() {
            return "[" + (slot + 1) + "] " + stack.getHoverName().getString();
        }
    }

    private record Plan(LivingEntity target, HotbarItem first, HotbarItem second, double reach,
                        double estimatedDamage, boolean inReach, List<String> suggestions) {
        static Plan empty() {
            return new Plan(null, null, null, 0, 0, false, List.of("No chain yet."));
        }

        boolean valid() {
            return first != null && second != null;
        }

        String chainString() {
            if (!valid()) return "none";
            return (first.slot() + 1) + " -> " + (second.slot() + 1);
        }
    }

    @Override
    public boolean hasControlWindow() {
        return true;
    }

    @Override
    public ModuleControlWindow createControlWindow() {
        return new AttributeSwapperControlWindow(this);
    }
}
