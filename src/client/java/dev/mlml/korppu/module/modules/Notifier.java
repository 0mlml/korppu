package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.config.ListSetting;
import dev.mlml.korppu.config.MultiSelectSetting;
import dev.mlml.korppu.event.Listener;
import dev.mlml.korppu.event.events.EntityDeathEvent;
import dev.mlml.korppu.event.events.EntitySpawnEvent;
import dev.mlml.korppu.event.events.PacketEvent;
import dev.mlml.korppu.event.events.PlayerGamemodeChangeEvent;
import dev.mlml.korppu.event.events.PlayerTrackEvent;
import dev.mlml.korppu.gui.ChatHelper;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.NotifierApi;
import dev.mlml.korppu.module.StatusEntry;
import dev.mlml.korppu.player.PlayerManager;
import dev.mlml.korppu.player.PlayerRelation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.core.registries.BuiltInRegistries;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class Notifier extends Module {
    private static final Path COORD_LOG_FILE = Path.of("korppu-coord-log.txt");
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DoubleSetting stayTime = config.add(
            new DoubleSetting("Stay Time", "How long notifications stay on screen (seconds)", 4d, 1d, 30d, 1));

    private final BooleanSetting notifyPlayerEnter = config.add(
            new BooleanSetting("Notify Player Enter", "Alert when a player enters render distance", true));
    private final BooleanSetting notifyPlayerLeave = config.add(
            new BooleanSetting("Notify Player Leave", "Alert when a player leaves render distance", true));

    private final BooleanSetting alertFriendEnter = config.add(
            new BooleanSetting("Alert Friend Enter", "Alert when a friend enters render distance", false));
    private final BooleanSetting alertEnemyEnter = config.add(
            new BooleanSetting("Alert Enemy Enter", "Alert when an enemy enters render distance", true));
    private final BooleanSetting alertOnProximity = config.add(
            new BooleanSetting("Alert on Proximity", "Alert when a friend/enemy is within proximity radius", true));
    private final DoubleSetting proximityRadius = config.add(
            new DoubleSetting("Proximity Radius", "Proximity alert radius in blocks", 32d, 4d, 256d, 0));
    private final List<UUID> nearbyRelations = new ArrayList<>();

    private final BooleanSetting notifyGamemodeChange = config.add(
            new BooleanSetting("Notify Gamemode Change", "Alert when any player's gamemode changes", true));

    private final BooleanSetting notifyEntityDeath = config.add(
            new BooleanSetting("Notify Entity Death", "Alert when an entity matching the filter dies", false));
    private final DoubleSetting entityDeathRange = config.add(
            new DoubleSetting("Death Notify Range", "Max distance for death alerts (0 = unlimited)", 0d, 0d, 512d, 0));
    private final MultiSelectSetting entityDeathFilter = config.add(
            new MultiSelectSetting("Death Entity Filter",
                    "Entity types to notify for death (empty = all entity types)",
                    () -> BuiltInRegistries.ENTITY_TYPE.stream()
                            .map(et -> BuiltInRegistries.ENTITY_TYPE.getKey(et).toString())
                            .filter(id -> !id.equals("minecraft:area_effect_cloud"))
                            .sorted()
                            .collect(Collectors.toList()),
                    true, false,
                    Set.of("minecraft:player")));

    private final BooleanSetting notifyEntitySpawn = config.add(
            new BooleanSetting("Notify Entity Spawn", "Alert when an entity matching the filter spawns", false));
    private final DoubleSetting entitySpawnRange = config.add(
            new DoubleSetting("Spawn Notify Range", "Max distance for spawn alerts (0 = unlimited)", 0d, 0d, 512d, 0));
    private final MultiSelectSetting entitySpawnFilter = config.add(
            new MultiSelectSetting("Spawn Entity Filter",
                    "Entity types to watch for spawning (empty = all non-player entities)",
                    () -> BuiltInRegistries.ENTITY_TYPE.stream()
                            .map(et -> BuiltInRegistries.ENTITY_TYPE.getKey(et).toString())
                            .filter(id -> !id.equals("minecraft:area_effect_cloud"))
                            .sorted()
                            .collect(Collectors.toList()),
                    true, false));

    private final BooleanSetting logTeleports = config.add(
            new BooleanSetting("Log Teleports", "Log incoming position-look teleports to file", false));
    private final BooleanSetting logTeleportDimension = config.add(
            new BooleanSetting("Log TP Dimension", "Include dimension id in teleport log", true)
                    .showOnlyIf(() -> logTeleports.getValue()));

    private final BooleanSetting antiVanish = config.add(
            new BooleanSetting("Anti Vanish", "Detect players leaving tab without visible leave message", false));
    private final ListSetting<AntiVanishMode> antiVanishMode = config.add(
            new ListSetting<>("Anti Vanish Mode", "Detection strategy", AntiVanishMode.LeaveMessage)
                    .showOnlyIf(() -> antiVanish.getValue()));
    private final DoubleSetting antiVanishInterval = config.add(
            new DoubleSetting("Anti Vanish Interval", "Ticks between vanish checks", 100d, 5d, 400d, 0)
                    .showOnlyIf(() -> antiVanish.getValue()));
    private final BooleanSetting antiVanishChatAlert = config.add(
            new BooleanSetting("Anti Vanish Chat Alert", "Also print vanish alerts to chat", true)
                    .showOnlyIf(() -> antiVanish.getValue()));

    private final BooleanSetting customPacketLog = config.add(
            new BooleanSetting("Custom Packet Log", "Notify packet activity (throttled)", false));
    private final BooleanSetting customPacketSent = config.add(
            new BooleanSetting("Custom Packet Sent", "Track outgoing packets", false)
                    .showOnlyIf(() -> customPacketLog.getValue()));
    private final BooleanSetting customPacketReceived = config.add(
            new BooleanSetting("Custom Packet Received", "Track incoming packets", true)
                    .showOnlyIf(() -> customPacketLog.getValue()));
    private final DoubleSetting customPacketInterval = config.add(
            new DoubleSetting("Custom Packet Interval", "Minimum ticks between packet notifications", 20d, 1d, 200d, 0)
                    .showOnlyIf(() -> customPacketLog.getValue()));

    private int loggedTeleports;
    private int vanishAlerts;
    private int packetAlerts;
    private int customPacketCooldown;
    private int vanishTimer;
    private final Map<UUID, String> tabSnapshot = new HashMap<>();
    private final List<String> recentMessages = new ArrayList<>();
    private final Random antiVanishRandom = new Random();
    private Integer pendingSuggestionId;
    private List<String> suggestionSnapshot = new ArrayList<>();

    public Notifier() {
        super("Notifier", "Desktop notifications for world events", GLFW.GLFW_KEY_UNKNOWN);
        KorppuMod.eventManager.register(this);
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        return List.of(
                StatusEntry.indicator("Player Enter/Leave", notifyPlayerEnter.getValue() || notifyPlayerLeave.getValue()),
                StatusEntry.indicator("Gamemode Change", notifyGamemodeChange.getValue()),
                StatusEntry.indicator("Entity Death", notifyEntityDeath.getValue()),
                StatusEntry.indicator("Entity Spawn", notifyEntitySpawn.getValue()),
                StatusEntry.indicator("Coord Logger", logTeleports.getValue()),
                StatusEntry.indicator("Anti Vanish", antiVanish.getValue()),
                StatusEntry.indicator("Custom Packets", customPacketLog.getValue()),
                StatusEntry.text("Packet Alerts", String.valueOf(packetAlerts)),
                StatusEntry.text("Vanish Alerts", String.valueOf(vanishAlerts)));
    }

    @Listener
    public void onGamemodeChange(PlayerGamemodeChangeEvent event) {
        if (!isEnabled() || !notifyGamemodeChange.getValue()) return;
        notify("Gamemode Change",
                event.getPlayerName() + ": " + event.getOldGameMode().name()
                        + " \u2192 " + event.getNewGameMode().name());
    }

    @Listener
    public void onPlayerEnter(PlayerTrackEvent.Enter event) {
        if (!isEnabled()) return;
        if (notifyPlayerEnter.getValue()) {
            notify("Player Entered Range", event.getPlayerName() + " entered render distance");
        }
        PlayerRelation rel = PlayerManager.getInstance().getRelation(event.getPlayerId());
        if (rel == PlayerRelation.FRIEND && alertFriendEnter.getValue()) {
            ChatHelper.message("§a[Friend] §f" + event.getPlayerName() + " §aentered render distance");
        } else if (rel == PlayerRelation.ENEMY && alertEnemyEnter.getValue()) {
            ChatHelper.message("§c[Enemy] §f" + event.getPlayerName() + " §centered render distance");
        }
    }

    @Listener
    public void onPlayerLeave(PlayerTrackEvent.Leave event) {
        if (!isEnabled() || !notifyPlayerLeave.getValue()) return;
        notify("Player Left Range", event.getPlayerName() + " left render distance");
    }

    @Override
    public void onDisable() {
        nearbyRelations.clear();
        tabSnapshot.clear();
        recentMessages.clear();
        vanishTimer = 0;
        pendingSuggestionId = null;
        suggestionSnapshot = new ArrayList<>();
        customPacketCooldown = 0;
    }

    @Override
    public void onTick() {
        if (alertOnProximity.getValue() && KorppuMod.mc.level != null && KorppuMod.mc.player != null) {
            double radius = proximityRadius.getValue();
            List<UUID> current = new ArrayList<>();
            for (Player player : KorppuMod.mc.level.players()) {
                if (player.getUUID().equals(KorppuMod.mc.player.getUUID())) continue;
                PlayerRelation rel = PlayerManager.getInstance().getRelation(player.getUUID());
                if (rel == PlayerRelation.NEUTRAL) continue;
                if (player.distanceTo(KorppuMod.mc.player) <= radius) {
                    current.add(player.getUUID());
                    if (!nearbyRelations.contains(player.getUUID())) {
                        String tag = rel == PlayerRelation.FRIEND ? "§a[Friend]" : "§c[Enemy]";
                        ChatHelper.message(tag + " §f" + player.getGameProfile().name()
                                + " §eis nearby! (" + String.format("%.0f", player.distanceTo(KorppuMod.mc.player)) + "m)");
                    }
                }
            }
            nearbyRelations.clear();
            nearbyRelations.addAll(current);
        }

        tickAntiVanish();
        if (customPacketCooldown > 0) {
            customPacketCooldown--;
        }
    }


    @Listener
    public void onEntityDeath(EntityDeathEvent event) {
        if (!isEnabled() || !notifyEntityDeath.getValue()) return;

        LivingEntity entity = event.getEntity();
        String typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();

        Set<String> filter = entityDeathFilter.getValue();
        if (!filter.isEmpty() && !filter.contains(typeId)) return;

        if (isNotInRange(entity, entityDeathRange.getValue())) return;

        String name = entity instanceof Player p
                ? p.getDisplayName().getString()
                : entity.getType().getDescription().getString();

        String cause = event.getSource().type().msgId();
        notify("Entity Death", name + " died (" + cause + ")");
    }

    @Listener
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (!isEnabled() || !notifyEntitySpawn.getValue()) return;

        if (event.getEntity() instanceof Player) return;

        String typeId = BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType()).toString();

        Set<String> filter = entitySpawnFilter.getValue();
        if (!filter.isEmpty() && !filter.contains(typeId)) return;

        if (isNotInRange(event.getEntity(), entitySpawnRange.getValue())) return;

        String typeName = event.getEntity().getType().getDescription().getString();
        notify("Entity Spawned", typeName + " spawned nearby");
    }

    @Listener
    public void onPacket(PacketEvent.Received event) {
        if (!isEnabled() || !logTeleports.getValue()) return;
        if (!(event.getPacket() instanceof ClientboundPlayerPositionPacket packet)) return;

        double[] xyz = extractCoordinates(packet);
        if (xyz == null) return;

        StringBuilder line = new StringBuilder();
        line.append("[").append(LocalDateTime.now().format(TS)).append("] ");
        if (logTeleportDimension.getValue() && KorppuMod.mc.level != null) {
            line.append(KorppuMod.mc.level.dimension().identifier()).append(" ");
        }
        line.append(String.format("x=%.2f y=%.2f z=%.2f", xyz[0], xyz[1], xyz[2]));

        try {
            Files.writeString(COORD_LOG_FILE, line + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            loggedTeleports++;
        } catch (IOException ignored) {
        }
    }

    @Listener
    public void onPacketAlertsReceived(PacketEvent.Received event) {
        if (!isEnabled() || !customPacketLog.getValue() || !customPacketReceived.getValue()) return;
        notifyPacket("RX", event.getPacket());
    }

    @Listener
    public void onPacketAlertsSent(PacketEvent.Sent event) {
        if (!isEnabled() || !customPacketLog.getValue() || !customPacketSent.getValue()) return;
        notifyPacket("TX", event.getPacket());
    }

    @Listener
    public void onMessagePacket(PacketEvent.Received event) {
        if (!isEnabled() || !antiVanish.getValue() || event.getPacket() == null) return;

        if (antiVanishMode.getValue() == AntiVanishMode.RealJoinMessage) {
            handleSuggestionPacket(event.getPacket());
        }

        String packetName = event.getPacket().getClass().getSimpleName();
        if (!packetName.contains("Message") && !packetName.contains("Chat")) return;

        for (var field : event.getPacket().getClass().getDeclaredFields()) {
            try {
                field.setAccessible(true);
                Object value = field.get(event.getPacket());
                if (value == null) continue;
                if (value instanceof String s && !s.isBlank()) {
                    recentMessages.add(s);
                } else {
                    String text = value.toString();
                    if (!text.isBlank() && text.length() < 500) {
                        recentMessages.add(text);
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        if (recentMessages.size() > 50) {
            recentMessages.subList(0, recentMessages.size() - 50).clear();
        }
    }

    private void tickAntiVanish() {
        if (!antiVanish.getValue() || KorppuMod.mc.getConnection() == null) return;

        vanishTimer++;
        int interval = antiVanishInterval.getValue().intValue();
        if (vanishTimer < interval) return;
        vanishTimer = 0;

        if (antiVanishMode.getValue() == AntiVanishMode.RealJoinMessage) {
            requestNameSuggestions();
            recentMessages.clear();
            return;
        }

        Collection<net.minecraft.client.multiplayer.PlayerInfo> entries = KorppuMod.mc.getConnection().getListedOnlinePlayers();
        Map<UUID, String> latest = new HashMap<>();
        for (var entry : entries) {
            if (entry.getProfile() == null) continue;
            latest.put(entry.getProfile().id(), entry.getProfile().name());
        }

        if (!tabSnapshot.isEmpty()) {
            for (Map.Entry<UUID, String> old : tabSnapshot.entrySet()) {
                if (latest.containsKey(old.getKey())) continue;

                String name = old.getValue();
                if (name == null || name.isBlank()) continue;
                if (name.length() < 3 || name.length() > 16 || name.contains(" ")) continue;

                boolean hasLeaveMessage = recentMessages.stream().anyMatch(msg -> msg.toLowerCase().contains(name.toLowerCase()));
                if (!hasLeaveMessage) {
                    vanishAlerts++;
                    notify("Possible Vanish", name + " disappeared from tab without a leave message");
                    if (antiVanishChatAlert.getValue()) {
                        ChatHelper.message("§6[AntiVanish] §f" + name + " may have vanished");
                    }
                }
            }
        }

        tabSnapshot.clear();
        tabSnapshot.putAll(latest);
        recentMessages.clear();
    }

    private void requestNameSuggestions() {
        if (KorppuMod.mc.getConnection() == null) return;

        try {
            int id = antiVanishRandom.nextInt(10_000_000);
            Class<?> clazz = Class.forName("net.minecraft.network.packet.c2s.play.RequestCommandCompletionsC2SPacket");
            Object packet = clazz.getDeclaredConstructor(int.class, String.class).newInstance(id, "/msg ");
            KorppuMod.mc.getConnection().send((net.minecraft.network.protocol.Packet<?>) packet);
            pendingSuggestionId = id;
        } catch (Throwable ignored) {
        }
    }

    private void handleSuggestionPacket(Object packet) {
        if (packet == null || pendingSuggestionId == null) return;
        if (!packet.getClass().getSimpleName().contains("CommandSuggestions")) return;

        Integer packetId = readSuggestionId(packet);
        if (packetId == null || !packetId.equals(pendingSuggestionId)) return;

        List<String> names = readSuggestionNames(packet);
        if (names.isEmpty()) {
            pendingSuggestionId = null;
            return;
        }

        handleSuggestionNames(names);
        pendingSuggestionId = null;
        recentMessages.clear();
    }

    public void ingestCommandSuggestions(List<String> names) {
        if (!isEnabled() || !antiVanish.getValue() || antiVanishMode.getValue() != AntiVanishMode.RealJoinMessage) {
            return;
        }
        if (names == null || names.isEmpty()) return;

        handleSuggestionNames(names);
    }

    private void handleSuggestionNames(List<String> names) {

        if (!suggestionSnapshot.isEmpty()) {
            for (String name : names) {
                if (!suggestionSnapshot.contains(name) && isValidPlayerName(name)) {
                    notify("Player Joined", name + " appeared in command suggestions");
                }
            }

            for (String name : suggestionSnapshot) {
                if (!names.contains(name) && isValidPlayerName(name)) {
                    boolean hasLeaveMessage = recentMessages.stream().anyMatch(msg -> msg.toLowerCase().contains(name.toLowerCase()));
                    if (!hasLeaveMessage) {
                        vanishAlerts++;
                        notify("Possible Vanish", name + " disappeared from command suggestions");
                        if (antiVanishChatAlert.getValue()) {
                            ChatHelper.message("§6[AntiVanish] §f" + name + " may have vanished");
                        }
                    }
                }
            }
        }

        suggestionSnapshot = new ArrayList<>(names);
    }

    private static Integer readSuggestionId(Object packet) {
        try {
            for (var method : packet.getClass().getMethods()) {
                if ((method.getName().equals("id") || method.getName().equals("getCompletionId"))
                        && method.getParameterCount() == 0
                        && (method.getReturnType() == int.class || method.getReturnType() == Integer.class)) {
                    Object value = method.invoke(packet);
                    return value instanceof Integer i ? i : null;
                }
            }

            for (var field : packet.getClass().getDeclaredFields()) {
                if (field.getType() == int.class && field.getName().toLowerCase().contains("id")) {
                    field.setAccessible(true);
                    return field.getInt(packet);
                }
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    private static List<String> readSuggestionNames(Object packet) {
        try {
            Object suggestions = null;
            for (String methodName : List.of("toSuggestions", "getSuggestions", "suggestions")) {
                try {
                    var method = packet.getClass().getMethod(methodName);
                    suggestions = method.invoke(packet);
                    if (suggestions != null) break;
                } catch (NoSuchMethodException ignored) {
                }
            }
            if (suggestions == null) return List.of();

            var getList = suggestions.getClass().getMethod("getList");
            Object listObj = getList.invoke(suggestions);
            if (!(listObj instanceof List<?> list)) return List.of();

            List<String> names = new ArrayList<>();
            for (Object suggestion : list) {
                try {
                    var getText = suggestion.getClass().getMethod("getText");
                    Object text = getText.invoke(suggestion);
                    if (text instanceof String s) names.add(s);
                } catch (Throwable ignored) {
                }
            }
            return names;
        } catch (Throwable ignored) {
            return List.of();
        }
    }

    private static boolean isValidPlayerName(String name) {
        return name != null && !name.isBlank() && !name.contains(" ") && name.length() >= 3 && name.length() <= 16;
    }

    public enum AntiVanishMode {
        LeaveMessage,
        RealJoinMessage
    }

    private static double[] extractCoordinates(ClientboundPlayerPositionPacket packet) {
        try {
            List<Double> values = new ArrayList<>();
            for (var field : packet.getClass().getDeclaredFields()) {
                if (field.getType() == double.class) {
                    field.setAccessible(true);
                    values.add(field.getDouble(packet));
                }
            }
            if (values.size() >= 3) return new double[]{values.get(0), values.get(1), values.get(2)};
        } catch (Throwable ignored) {
        }
        return null;
    }

    private void notify(String title, String message) {
        NotifierApi.push(title, message, (long) (stayTime.getValue() * 1000));
    }

    private boolean isNotInRange(net.minecraft.world.entity.Entity entity, double range) {
        if (range <= 0 || KorppuMod.mc.player == null) return false;
        return !(entity.distanceToSqr(KorppuMod.mc.player) <= range * range);
    }

    private void notifyPacket(String dir, Object packet) {
        if (packet == null || customPacketCooldown > 0) return;
        customPacketCooldown = customPacketInterval.getValue().intValue();
        packetAlerts++;
        notify("Packet " + dir, packet.getClass().getSimpleName());
    }
}
