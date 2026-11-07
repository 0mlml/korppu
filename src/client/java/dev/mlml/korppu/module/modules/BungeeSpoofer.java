package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.config.StringSetting;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.StatusEntry;
import lombok.Getter;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class BungeeSpoofer extends Module {
    @Getter
    private final StringSetting spoofedAddress = config.add(new StringSetting(
            "Spoofed Address", "IP address to spoof in the BungeeCord handshake", "127.0.0.1"));

    public BungeeSpoofer() {
        super("BungeeSpoofer", "Spoofs the BungeeCord handshake IP to allow connecting to misconfigured backend servers. Only enable when intentionally exploiting an exposed backend.", GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        return List.of(StatusEntry.text("Spoof Address", spoofedAddress.getValue()));
    }

    @Override
    public String getStatus() {
        return spoofedAddress.getValue();
    }
}
