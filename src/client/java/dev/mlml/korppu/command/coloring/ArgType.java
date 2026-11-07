package dev.mlml.korppu.command.coloring;

import lombok.Getter;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.awt.*;

public enum ArgType {
    STRING(new Color(0x55FF55), String.class),
    NUMBER(new Color(0x009DFF), Integer.class, Double.class, Float.class, Long.class),
    PLAYER(new Color(0xFF9900), Player.class, AbstractClientPlayer.class, LivingEntity.class);
    @Getter
    final Color color;
    final Class<?>[] appliesTo;

    ArgType(Color color, Class<?>... appliesToClass) {
        this.color = color;
        this.appliesTo = appliesToClass;
    }

}