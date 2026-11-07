package dev.mlml.korppu.command.commands;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.command.Command;
import dev.mlml.korppu.command.exception.CommandException;
import dev.mlml.korppu.imgui.KorppuImGui;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;

public class ViewNbt extends Command {
    public ViewNbt() {
        super("viewnbt", "Opens item component/NBT data in the editor", "nbt");
    }

    @Override
    public void execute(String[] args) throws CommandException {
        if (KorppuMod.mc.player == null) throw new CommandException("Not in a world");

        ItemStack held = KorppuMod.mc.player.getMainHandItem();
        if (held.isEmpty()) throw new CommandException("Not holding an item");

        String itemId = BuiltInRegistries.ITEM.getKey(held.getItem()).toString();
        StringBuilder sb = new StringBuilder();
        sb.append("Item: ").append(itemId).append(" x").append(held.getCount()).append("\n\n");

        var components = held.getComponents();
        if (components.isEmpty()) {
            sb.append("No component data.");
        } else {
            sb.append("Components (").append(components.size()).append("):\n");
            for (DataComponentType<?> type : components.keySet()) {
                String name = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type).toString();
                Object value = components.get(type);
                sb.append(name).append(" =\n  ").append(value).append("\n\n");
            }
        }

        KorppuImGui.openEditorWithContent(sb.toString());
    }
}
