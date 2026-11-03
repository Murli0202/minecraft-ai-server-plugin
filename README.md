# Minecraft AI Server Plugin

A Paper plugin that sends player questions to a local Ollama instance and posts the answer in Minecraft chat.

## Features

- Chat command integration with Ollama
- Local-only AI endpoint validation
- Configurable command prefix and prompt text
- Config reload command
- AI server override command
- Cow protection toggle

## Requirements

- Paper server
- Java 25
- Ollama running locally on the same machine

## Installation

```bash
git clone https://github.com/Murli0202/minecraft-ai-server-plugin.git
cd minecraft-ai-server-plugin
mvn package -q
cp target/ki-plugin-*.jar /path/to/server/plugins/
```

Restart the Paper server and the plugin will load automatically.

## Usage

In chat:

```text
!ki What is a Creeper?
!ki How do I build a portal?
```

The plugin sends the prompt to the configured local Ollama endpoint and broadcasts the answer in chat.

## Commands

- `!ki <question>` — ask the AI
- `/kireload` — reload the config
- `/setaiserver <url>` — set the AI server URL (localhost only)

## Configuration

The plugin reads its config from `plugins/KiPlugin/config.yml` after the first start.

Default configuration:

```yml
befehl: "!ki"
modell: "mistral"
ollama-url: "http://localhost:11434/api/generate"
prompt-prefix: "Antworte kurz auf Deutsch, maximal 2 Saetze: "
chat-prefix: "§b[KI] §f"
warte-meldung: "§7Einen Moment..."
hilfe-meldung: "Beispiel: !ki Was ist ein Creeper?"
fehler-meldung: "§cFehler beim Abrufen der KI."
timeout-sekunden: 60
nur-anfrager-sieht-antwort: false

cow-protection:
  enabled: true
  kick-message: "§cDon't kill cows on this server"
  death-message: "§e[Cow Protection] §fA cow has died!"
```

## Security

Only localhost endpoints are allowed for AI requests.

Examples:

- `http://localhost:11434/api/generate`
- `http://127.0.0.1:11434/api/generate`
- `http://[::1]:11434/api/generate`

Remote hosts are rejected.

## License

MIT
