# Minecraft AI Server Plugin

A Paper plugin that lets players ask a local Ollama instance questions directly in Minecraft chat.

This plugin is intentionally limited to localhost-based AI endpoints for security. Remote AI hosts are rejected to prevent SSRF-style abuse.

## Features

- Chat command integration with Ollama
- Local-only AI endpoint validation
- Configurable command prefix and prompt text
- Cow protection feature
- Join-time update notification for OPs
- Config reload command

## Requirements

- Paper server
- Java 25
- Ollama installed locally on the same machine or a localhost-only reachable instance

Example:

```bash
ollama pull mistral
ollama serve
```

## Installation

### Build from source

```bash
git clone https://github.com/Murli0202/minecraft-ai-server-plugin.git
cd minecraft-ai-server-plugin
mvn package -q
cp target/ki-plugin-*.jar /path/to/server/plugins/
```

### Restart the server

```bash
/stop
```

Then start the server again and let Paper load the plugin.

## Usage

In chat, type:

```text
!ki What is a Creeper?
!ki How do I build a portal?
!ki Explain redstone
```

The plugin sends the prompt to the configured local Ollama endpoint and broadcasts the response in chat.

## Commands

- `!ki <question>` — ask the AI
- `/kireload` — reload the config without restarting the server
- `/setaiserver <url>` — set the AI server URL (must be localhost-only)

## Configuration

The plugin reads its settings from `plugins/KiPlugin/config.yml` after the first start.

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

Only localhost endpoints are permitted for AI requests.

Allowed examples:

- `http://localhost:11434/api/generate`
- `http://127.0.0.1:11434/api/generate`
- `http://[::1]:11434/api/generate`

Blocked examples:

- `http://example.com/...`
- `https://8.8.8.8/...`
- any non-localhost remote host

This restriction exists to prevent outbound requests to arbitrary internal or external services from the server process.

## Override behavior

The chat override syntax is still supported, but it is restricted to localhost-based URLs only.

Examples:

```text
!ki@localhost:11434 Was ist ein Creeper?
!ki@127.0.0.1:11434 Erkläre Redstone
```

If a remote host is provided, the request is denied.

## External AI provider model examples

The model IDs below are examples of commonly used provider-specific model names. Exact model availability changes over time, so check your provider dashboard before using a specific model.

### OpenAI

- `gpt-4o`
- `gpt-4o-mini`
- `gpt-4.1`
- `gpt-4.1-mini`
- `o3-mini`

### Anthropic

- `claude-3-5-sonnet-latest`
- `claude-3-5-haiku-latest`
- `claude-3-opus-latest`

### Cohere

- `command-r-plus`
- `command-r`

### Google Gemini

- `gemini-2.0-flash`
- `gemini-1.5-pro`

### xAI Grok

- `grok-2-latest`
- `grok-2-mini-latest`

Example config block:

```yml
provider: "external"

external-ai:
  service: "openai"
  api-key: "your-api-key"
  model: "gpt-4o"
  max-tokens: 100
  temperature: 0.7
```

## Troubleshooting

### Ollama not responding

- Make sure Ollama is running with `ollama serve`
- Verify the model exists with `ollama list`
- Check that the endpoint is reachable on localhost

### Plugin cannot reach AI

- Confirm `ollama-url` is set to a valid localhost URL
- Check that the configured model is installed
- Review the server logs for request/response errors

## License

MIT
