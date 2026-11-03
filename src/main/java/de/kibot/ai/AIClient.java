package de.kibot.ai;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class AIClient {
    private volatile String serverUrl;
    private final int timeoutMillis;

    public AIClient(String serverUrl, int timeoutMillis) {
        this.serverUrl = serverUrl;
        this.timeoutMillis = timeoutMillis;
    }

    public synchronized void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    // Sends a POST request with the given JSON payload to the configured server.
    // endpointPath may be "/api/generate" or null/empty if serverUrl already contains the path.
    public String ask(String endpointPath, String jsonPayload) throws IOException {
        String base = serverUrl == null ? "" : serverUrl;
        String target = base;
        if (endpointPath != null && !endpointPath.isEmpty()) {
            if (!base.endsWith("/")) target = base + endpointPath;
            else target = base.substring(0, base.length()-1) + endpointPath;
        }

        URL url = new URL(target);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(timeoutMillis);
        conn.setReadTimeout(timeoutMillis);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setDoOutput(true);

        byte[] payloadBytes = jsonPayload.getBytes(StandardCharsets.UTF_8);
        conn.setFixedLengthStreamingMode(payloadBytes.length);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(payloadBytes);
        }

        int status = conn.getResponseCode();
        InputStream in = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
        if (in == null) return "";

        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append("\n");
            return sb.toString().trim();
        } finally {
            conn.disconnect();
        }
    }

    // Convenience method for servers that expect {"prompt":"..."}
    public String askPrompt(String endpointPath, String prompt) throws IOException {
        String payload = "{\"prompt\":\"" + escapeJson(prompt) + "\"}";
        return ask(endpointPath, payload);
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
