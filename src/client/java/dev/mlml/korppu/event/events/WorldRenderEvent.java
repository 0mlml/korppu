package dev.mlml.korppu.event.events;

import dev.mlml.korppu.event.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import com.mojang.blaze3d.vertex.PoseStack;

@Getter
@Setter
@AllArgsConstructor
public class WorldRenderEvent extends Event {
    PoseStack matrixStack;
    float tickDelta;
}
