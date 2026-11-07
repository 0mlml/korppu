package dev.mlml.korppu.event.events;

import dev.mlml.korppu.event.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;

@Getter
@Setter
@AllArgsConstructor
public class EntityRenderEvent extends Event {
    GuiGraphicsExtractor drawContext;
    Entity entity;
}
