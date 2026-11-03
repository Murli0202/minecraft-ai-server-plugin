package de.kibot;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Scanner;

public class KiPlugin extends JavaPlugin implements Listener {

    private static final String PLUGIN_VERSION = "1.2";
    private static final String DEFAULT_OLLAMA_URL = "http://localhost:11434/api/generate";

    private String neuesteVersion = null;
    private boolean updateVerfuegbar = false;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("KI-Plugin v" + PLUGIN_VERSION + " gestartet! Befehl: " + getBefehl());
        getServer().getScheduler().runTaskAsynchronously(this, this::pruefeAufUpdates);
    }

    private String getBefehl()        { return getConfig().getString("befehl", "!ki"); }
    private String getModell()        { return getConfig().getString("modell", "mistral"); }
    private String getOllamaUrl()     { return getConfig().getString("ollama-url", DEFAULT_OLLAMA_URL); }
    private String getPromptPrefix()  { return getConfig().getString("prompt-prefix", "Antworte kurz auf Deutsch, maximal 2 Saetze: "); }
    private String getChatPrefix()    { return getConfig().getString("chat-prefix", "§b[KI] §f"); }
    private String getWarteMeldung()  { return getConfig().getString("warte-meldung", "§7Einen Moment..."); }
    private String getHilfeMeldung()  { return getConfig().getString("hilfe-meldung", "Beispiel: !ki Was ist ein Creeper?"); }
    private String getFehlerMeldung() { return getConfig().getString("fehler-meldung", "§cFehler beim Abrufen der KI."); }
    private int    getTimeout()       { return getConfig().getInt("timeout-sekunden", 60) * 1000; }
    private boolean nurAnfrager()     { return getConfig().getBoolean("nur-anfrager-sieht-antwort", false); }

    private boolean isCowProtectionEnabled() { return getConfig().getBoolean("cow-protection.enabled", true); }
    private String getCowKickMessage() { return getConfig().getString("cow-protection.kick-message", "§cDon't kill cows on this server"); }
    private String getCowDeathMessage() { return getConfig().getString("cow-protection.death-message", "§e[Cow Protection] §fA cow has died!"); }

    private String normalizeAiServerUrl(String rawUrl) {
        String candidate = rawUrl == null ? "" : rawUrl.trim();
        if (candidate.isEmpty()) {
            return DEFAULT_OLLAMA_URL;
        }
        if (!candidate.startsWith("http://") && !candidate.startsWith("https://")) {
            candidate = "http://" + candidate;
        }
        try {
            URL parsed = new URL(candidate);
            String protocol = parsed.getProtocol();
            String host = parsed.getHost();
            if (host == null || host.isEmpty()) {
                throw new IllegalArgumentException("AI URL must include a hostname.");
            }
            if (!"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol)) {
                throw new IllegalArgumentException("Only http and https are allowed for AI URLs.");
            }
            String normalizedHost = host.toLowerCase(Locale.ROOT);
            boolean allowed = "localhost".equals(normalizedHost)
                    || "127.0.0.1".equals(normalizedHost)
                    || "::1".equals(normalizedHost)
                    || "[::1]".equals(normalizedHost);
            if (!allowed) {
                throw new IllegalArgumentException("Only localhost endpoints are allowed for AI requests.");
            }
            return parsed.toString();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid AI server URL: " + rawUrl, e);
        }
    }

    private String ensureOllamaEndpoint(String rawUrl) {
        String normalized = normalizeAiServerUrl(rawUrl);
        if (normalized.endsWith("/api/generate")) {
            return normalized;
        }
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized + "/api/generate";
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '\\': sb.append("\\\\"); break;
                case '"': sb.append("\\\""); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (ch < 0x20) {
                        sb.append(String.format("\\u%04x", (int) ch));
                    } else {
                        sb.append(ch);
                    }
                    break;
            }
        }
        return sb.toString();
    }

    private void pruefeAufUpdates() {
        try {
            String apiUrl = "https://api.github.com/repos/Murli0202/minecraft-ai-server-plugin/releases/latest";
            HttpURLConnection con = (HttpURLConnection) new URL(apiUrl).openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Accept", "application/vnd.github+json");
            con.setRequestProperty("User-Agent", "KiPlugin/" + PLUGIN_VERSION);
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);

            if (con.getResponseCode() != 200) {
                getLogger().info("[Update] Konnte GitHub nicht erreichen (Code: " + con.getResponseCode() + ")");
                return;
            }

            try (Scanner sc = new Scanner(con.getInputStream(), StandardCharsets.UTF_8)) {
                StringBuilder sb = new StringBuilder();
                while (sc.hasNextLine()) sb.append(sc.nextLine());
                String json = sb.toString();

                int start = json.indexOf("\"tag_name\":\"") + 12;
                int end   = json.indexOf("\"", start);
                if (start < 12 || end < 0) return;

                neuesteVersion = json.substring(start, end).replace("v", "").trim();

                if (!neuesteVersion.equals(PLUGIN_VERSION)) {
                    updateVerfuegbar = true;
                    getLogger().info("[Update] Neue Version verfügbar: v" + neuesteVersion + " (aktuell: v" + PLUGIN_VERSION + ")");
                    getLogger().info("[Update] https://github.com/Murli0202/minecraft-ai-server-plugin/releases/latest");
                } else {
                    getLogger().info("[Update] Plugin ist aktuell (v" + PLUGIN_VERSION + ")");
                }
            }

        } catch (Exception e) {
            getLogger().warning("[Update] Update-Check fehlgeschlagen: " + e.getMessage());
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!updateVerfuegbar) return;
        if (!event.getPlayer().isOp()) return;

        getServer().getScheduler().runTaskLater(this, () -> {
            event.getPlayer().sendMessage("§6§l[KI-Plugin] §eNeue Version verfügbar: §av" + neuesteVersion);
            event.getPlayer().sendMessage("§7Aktuell: §cv" + PLUGIN_VERSION + " §7→ Neu: §av" + neuesteVersion);
            event.getPlayer().sendMessage("§7Download: §bhttps://github.com/Murli0202/minecraft-ai-server-plugin/releases/latest");
        }, 40L);
    }

    @EventHandler
    public void onCowDeath(EntityDeathEvent event) {
        if (!isCowProtectionEnabled()) return;

        Entity entity = event.getEntity();
        if (!(entity instanceof Cow)) return;

        LivingEntity cow = (LivingEntity) entity;
        Player killer = cow.getKiller();

        if (killer != null) {
            killer.kickPlayer(getCowKickMessage());
            getLogger().info("[Cow Protection] Player " + killer.getName() + " was kicked for killing a cow");
        }

        getServer().broadcastMessage(getCowDeathMessage());
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        String nachricht = event.getMessage();
        if (!nachricht.startsWith(getBefehl())) return;

        String frage = nachricht.substring(getBefehl().length()).trim();
        if (frage.isEmpty()) {
            event.getPlayer().sendMessage("§e" + getHilfeMeldung());
            return;
        }

        String overrideUrl = null;
        if (frage.startsWith("@")) {
            int space = frage.indexOf(' ');
            String hostPart;
            String rest = "";
            if (space > 0) {
                hostPart = frage.substring(1, space).trim();
                rest = frage.substring(space).trim();
            } else {
                hostPart = frage.substring(1).trim();
            }

            if (hostPart.isEmpty()) {
                event.getPlayer().sendMessage("§e" + getHilfeMeldung());
                return;
            }

            try {
                overrideUrl = ensureOllamaEndpoint(hostPart);
                if (!rest.isEmpty()) {
                    frage = rest;
                }
            } catch (IllegalArgumentException e) {
                event.getPlayer().sendMessage("§cNur localhost-Server sind erlaubt. ");
                getLogger().warning("Blocked disallowed AI override URL: " + hostPart);
                return;
            }
        }

        event.getPlayer().sendMessage(getWarteMeldung());

        String fragenFinal = frage;
        String finalOverrideUrl = overrideUrl;
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            try {
                String antwort = frageOllama(fragenFinal, finalOverrideUrl);
                getServer().getScheduler().runTask(this, () -> {
                    if (nurAnfrager()) {
                        event.getPlayer().sendMessage(getChatPrefix() + antwort);
                    } else {
                        getServer().broadcastMessage(getChatPrefix() + antwort);
                    }
                });
            } catch (Exception e) {
                getLogger().severe("Ollama Fehler: " + e.getMessage());
                event.getPlayer().sendMessage(getFehlerMeldung());
            }
        });
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("kireload")) {
            if (!sender.hasPermission("kiplugin.reload")) {
                sender.sendMessage("§cKeine Berechtigung!");
                return true;
            }
            reloadConfig();
            sender.sendMessage("§aKI-Plugin Config neu geladen!");
            getServer().getScheduler().runTaskAsynchronously(this, this::pruefeAufUpdates);
            return true;
        }
        if (cmd.getName().equalsIgnoreCase("setaiserver")) {
            if (!sender.hasPermission("kiplugin.setaiserver")) {
                sender.sendMessage("§cKeine Berechtigung.");
                return true;
            }
            if (args.length == 0) {
                sender.sendMessage("§eVerwendung: /setaiserver <url>");
                return true;
            }
            try {
                String url = ensureOllamaEndpoint(args[0]);
                getConfig().set("ollama-url", url);
                saveConfig();
                sender.sendMessage("§aAI-Server gesetzt auf: " + url);
                return true;
            } catch (IllegalArgumentException e) {
                sender.sendMessage("§cNur localhost-Server sind erlaubt.");
                return true;
            }
        }
        return false;
    }

    private String frageOllama(String frage, String overrideOllamaUrl) throws Exception {
        String targetUrl = (overrideOllamaUrl != null && !overrideOllamaUrl.isEmpty()) ? ensureOllamaEndpoint(overrideOllamaUrl) : ensureOllamaEndpoint(getOllamaUrl());

        String body = "{\"model\":\"" + escapeJson(getModell()) + "\"," +
                "\"prompt\":\"" + escapeJson(getPromptPrefix() + frage) + "\"," +
                "\"stream\":false}";

        HttpURLConnection con = (HttpURLConnection) new URL(targetUrl).openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setDoOutput(true);
        con.setConnectTimeout(30000);
        con.setReadTimeout(getTimeout());

        try (OutputStream os = con.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        int status = con.getResponseCode();
        if (status < 200 || status >= 300) {
            throw new IllegalStateException("AI request failed with HTTP status " + status);
        }

        try (Scanner sc = new Scanner(con.getInputStream(), StandardCharsets.UTF_8)) {
            StringBuilder sb = new StringBuilder();
            while (sc.hasNextLine()) sb.append(sc.nextLine());
            String response = sb.toString();
            int start = response.indexOf("\"response\":\"") + 12;
            int end = response.indexOf("\",\"done\"");
            if (start < 12 || end < start) {
                throw new IllegalStateException("Unexpected Ollama response format.");
            }
            return response.substring(start, end)
                    .replace("\\n", " ")
                    .replace("\\\"", "\"")
                    .trim();
        }
    }
}
