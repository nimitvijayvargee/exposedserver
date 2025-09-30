package com.nimitapps.exposedserver;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.world.ServerWorld;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Exposedserver implements ModInitializer {

    public static final String MOD_ID = "exposedserver";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final Gson GSON = new GsonBuilder().create();

    private static String apiKey;
    private static MinecraftServer serverInstance;
    private static WSHandler wsServer;
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Override
    public void onInitialize() {
        try {
            File configFile = new File("config/exposedserver.json");
            if (!configFile.exists()) {
                configFile.getParentFile().mkdirs();
                configFile.createNewFile();
            }

            Map<String, Object> config = GSON.fromJson(new FileReader(configFile),
                    new TypeToken<Map<String, Object>>() {}.getType());

            apiKey = (String) config.getOrDefault("apiKey", "changeme");
            int port = ((Number) config.getOrDefault("port", 6767)).intValue();

            ServerLifecycleEvents.SERVER_STARTED.register(server -> {
                serverInstance = server;
                startWebSocket(port);
                scheduler.scheduleAtFixedRate(Exposedserver::broadcastStatus, 0, 1, TimeUnit.SECONDS);
            });

            ServerLifecycleEvents.SERVER_STOPPING.register(server -> stopWebSocket());

        } catch (Exception e) {
            LOGGER.error("[{}] Failed to initialize", MOD_ID, e);
        }
    }

    private static void startWebSocket(int port) {
        if (wsServer != null) return;
        try {
            wsServer = new WSHandler(new InetSocketAddress(port));
            wsServer.start();
            LOGGER.info("[{}] WebSocket API started on port {}", MOD_ID, port);
        } catch (Exception e) {
            LOGGER.error("[{}] Failed to start WebSocket", MOD_ID, e);
        }
    }

    private static void stopWebSocket() {
        if (wsServer != null) {
            try {
                wsServer.stop();
                wsServer = null;
                LOGGER.info("[{}] WebSocket stopped", MOD_ID);
            } catch (Exception e) {
                LOGGER.error("[{}] Failed to stop WebSocket", MOD_ID, e);
            }
        }
    }

    private static void broadcastStatus() {
        if (serverInstance == null || wsServer == null) return;

        PlayerManager pm = serverInstance.getPlayerManager();
        int playerCount = pm.getPlayerList().size();
        int maxPlayerCount = pm.getMaxPlayerCount();
        String version = serverInstance.getVersion();
        String motd = serverInstance.getServerMotd();

        int entityCount = 0;
        for (ServerWorld world : serverInstance.getWorlds()) {
            entityCount += (int) world.iterateEntities().spliterator().getExactSizeIfKnown();
        }

        int chunksLoaded = 0;
        for (ServerWorld world : serverInstance.getWorlds()) {
            chunksLoaded += world.getChunkManager().getLoadedChunkCount();
        }

        double avgMspt = serverInstance.getAverageNanosPerTick() / 1_000_000.0;

        LinkedHashMap<String, String> players = new LinkedHashMap<>();
        serverInstance.getPlayerManager().getPlayerList().forEach(player -> players.put(player.getUuidAsString(), player.getName().getString()));

        long usedRam = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long maxRam = Runtime.getRuntime().maxMemory();

        Map<String, Object> payload = new LinkedHashMap<>();

        payload.put("type", "status");
        payload.put("playerCount", playerCount);
        payload.put("maxPlayerCount", maxPlayerCount);
        payload.put("players", players);
        payload.put("version", version);
        payload.put("motd", motd);
        payload.put("entityCount", entityCount);
        payload.put("chunks", chunksLoaded);
        payload.put("mspt", avgMspt);
        payload.put("ram", usedRam / 1000);
        payload.put("maxRam", maxRam / 1000);


        String json = GSON.toJson(payload);

        for (WebSocket conn : wsServer.getConnections()) {
            conn.send(json);
        }
    }

    private static void executeCommand(String cmd, WebSocket conn) {
        if (serverInstance == null || cmd == null || cmd.isEmpty()) return;
        try {
            CommandManager manager = serverInstance.getCommandManager();
            manager.getDispatcher().execute(cmd, serverInstance.getCommandSource());
            conn.send("{\"success\":\"command executed\"}");
        } catch (Exception e) {
            conn.send("{\"error\":\"failed to execute\"}");
        }
    }

    private static class WSHandler extends WebSocketServer {
        public WSHandler(InetSocketAddress addr) {
            super(addr);
        }

        @Override
        public void onStart() {
            LOGGER.info("WebSocket server started successfully");
        }

        @Override
        public void onOpen(WebSocket conn, ClientHandshake handshake) {
            LOGGER.info("WebSocket client connected: {}", conn.getRemoteSocketAddress());
        }

        @Override
        public void onMessage(WebSocket conn, String message) {
            try {
                Map<String, String> req = GSON.fromJson(message, Map.class);
                if (!apiKey.equals(req.get("apiKey"))) {
                    conn.send("{\"error\":\"unauthorized\"}");
                    return;
                }
                if (req.containsKey("command")) {
                    executeCommand(req.get("command"), conn);
                }
            } catch (Exception e) {
                conn.send("{\"error\":\"bad_request\"}");
            }
        }

        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {}

        @Override
        public void onError(WebSocket conn, Exception ex) {}
    }
}
