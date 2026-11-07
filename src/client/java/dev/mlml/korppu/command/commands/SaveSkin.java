package dev.mlml.korppu.command.commands;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.command.Command;
import dev.mlml.korppu.command.coloring.ArgType;
import dev.mlml.korppu.command.coloring.PossibleArg;
import dev.mlml.korppu.command.exception.CommandException;
import dev.mlml.korppu.imgui.windows.ConsoleWindow;
import net.minecraft.client.multiplayer.PlayerInfo;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SaveSkin extends Command {
    private static final Pattern VALUE_PATTERN = Pattern.compile("\\\"value\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern URL_PATTERN = Pattern.compile("\\\"url\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");

    public SaveSkin() {
        super("saveskin", "Downloads and saves a player's current skin", "skin");
    }

    @Override
    public PossibleArg getSuggestionsWithType(int index, String[] args) {
        if (index == 0) {
            if (KorppuMod.mc.getConnection() == null) return new PossibleArg(ArgType.PLAYER);
            return new PossibleArg(ArgType.PLAYER,
                    KorppuMod.mc.getConnection().getListedOnlinePlayers().stream()
                            .map(e -> e.getProfile().name())
                            .toArray(String[]::new));
        }
        return super.getSuggestionsWithType(index, args);
    }

    @Override
    public void execute(String[] args) throws CommandException {
        if (KorppuMod.mc.player == null) throw new CommandException("Not in world.");

        String targetName;
        UUID targetUuid;
        if (args.length >= 1) {
            String lookupName = args[0];
            PlayerInfo entry = KorppuMod.mc.getConnection() == null ? null :
                    KorppuMod.mc.getConnection().getListedOnlinePlayers().stream()
                            .filter(e -> e.getProfile().name().equalsIgnoreCase(lookupName))
                            .findFirst().orElse(null);
            if (entry == null) throw new CommandException("Player not found in tab list: " + lookupName);
            targetName = entry.getProfile().name();
            targetUuid = entry.getProfile().id();
        } else {
            targetName = KorppuMod.mc.player.getGameProfile().name();
            targetUuid = KorppuMod.mc.player.getUUID();
        }

        String uuidNoDashes = targetUuid.toString().replace("-", "");
        HttpClient client = HttpClient.newHttpClient();

        String profileJson;
        try {
            HttpRequest profileReq = HttpRequest.newBuilder()
                    .uri(URI.create("https://sessionserver.mojang.com/session/minecraft/profile/" + uuidNoDashes))
                    .GET()
                    .build();
            profileJson = client.send(profileReq, HttpResponse.BodyHandlers.ofString()).body();
        } catch (Exception e) {
            throw new CommandException("Failed to fetch profile: " + e.getMessage());
        }

        Matcher valueMatcher = VALUE_PATTERN.matcher(profileJson);
        if (!valueMatcher.find()) throw new CommandException("No skin texture property found.");

        String decoded = new String(Base64.getDecoder().decode(valueMatcher.group(1)), StandardCharsets.UTF_8);
        Matcher urlMatcher = URL_PATTERN.matcher(decoded);
        if (!urlMatcher.find()) throw new CommandException("No skin URL found in profile data.");

        String skinUrl = urlMatcher.group(1);
        byte[] png;
        try {
            HttpRequest skinReq = HttpRequest.newBuilder().uri(URI.create(skinUrl)).GET().build();
            png = client.send(skinReq, HttpResponse.BodyHandlers.ofByteArray()).body();
        } catch (Exception e) {
            throw new CommandException("Failed to download skin PNG: " + e.getMessage());
        }

        Path outDir = Path.of("korppu-skins");
        Path outFile = outDir.resolve(targetName + "-" + uuidNoDashes + ".png");
        try {
            Files.createDirectories(outDir);
            Files.write(outFile, png);
        } catch (IOException e) {
            throw new CommandException("Failed to write file: " + e.getMessage());
        }

        ConsoleWindow.print("Saved skin to " + outFile.toAbsolutePath());
    }
}
