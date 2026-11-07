package dev.mlml.korppu.event.events;

import dev.mlml.korppu.event.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.GuiGraphicsExtractor;

@Getter
@Setter
@AllArgsConstructor
public class HudRenderEvent extends Event {
    GuiGraphicsExtractor context;
    float tickProgress;
}
