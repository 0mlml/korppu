package dev.mlml.korppu.command.commands;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.command.Command;
import dev.mlml.korppu.command.coloring.ArgType;
import dev.mlml.korppu.command.coloring.PossibleArg;
import dev.mlml.korppu.command.exception.CommandException;
import dev.mlml.korppu.imgui.windows.ConsoleWindow;
import dev.mlml.korppu.mixin.IPlayerInventoryMixin;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public class Give extends Command {
    public Give() {
        super("give", "Gives an item stack in creative mode", "item");
    }

    @Override
    public PossibleArg getSuggestionsWithType(int index, String[] args) {
        return switch (index) {
            case 0 -> new PossibleArg(ArgType.STRING,
                    BuiltInRegistries.ITEM.stream().map(item -> BuiltInRegistries.ITEM.getKey(item).toString()).toArray(String[]::new));
            case 1 -> new PossibleArg(ArgType.NUMBER, "count");
            default -> super.getSuggestionsWithType(index, args);
        };
    }

    @Override
    public void execute(String[] args) throws CommandException {
        if (KorppuMod.mc.player == null || KorppuMod.mc.getConnection() == null) throw new CommandException("Not in world.");
        if (!KorppuMod.mc.player.getAbilities().instabuild) throw new CommandException("Creative mode required.");
        validateArgsLength(args, 1, "Usage: give <item_id> [count]");

        Identifier id;
        try {
            id = Identifier.parse(args[0]);
        } catch (Exception e) {
            throw new CommandException("Invalid item id: " + args[0]);
        }

        Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        if (item == null || item == Items.AIR) throw new CommandException("Unknown item: " + args[0]);

        int count = 1;
        if (args.length >= 2) {
            try {
                count = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                throw new CommandException("Invalid count: " + args[1]);
            }
        }
        count = Math.max(1, Math.min(count, item.getDefaultMaxStackSize()));

        ItemStack stack = new ItemStack(item, count);
        int selected = ((IPlayerInventoryMixin) (Object) KorppuMod.mc.player.getInventory()).getSelectedSlot();
        KorppuMod.mc.player.getInventory().setItem(selected, stack.copy());
        KorppuMod.mc.getConnection().send(new ServerboundSetCreativeModeSlotPacket(36 + selected, stack));
        ConsoleWindow.print("Given: " + stack.getCount() + "x " + id);
    }
}
