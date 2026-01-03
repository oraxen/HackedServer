# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

HackedServer is a Minecraft plugin that detects hacked clients and mods by analyzing custom payload packets. It supports Spigot/Paper, BungeeCord, and Velocity servers.

## Build Commands

### Building the Plugin
```bash
# Build the plugin (creates shadowed jar)
./gradlew shadowJar

# Clean build
./gradlew clean shadowJar
```

The output jar is located at `build/libs/hackedserver-<version>.jar`.

## Architecture

### Multi-Platform Support

HackedServer uses a multi-module architecture:
- **hackedserver-core**: Shared logic, configuration, and detection algorithms
- **hackedserver-spigot**: Spigot/Paper implementation using ProtocolLib
- **hackedserver-bungeecord**: BungeeCord proxy implementation
- **hackedserver-velocity**: Velocity proxy implementation using PacketEvents

### Detection System

The plugin detects clients through:
1. **Generic Checks**: Pattern matching on custom payload channels/messages (configured in `generic.toml`)
2. **Forge Mod List**: Detection of Forge mods via FML handshake (configured in `forge.toml`)
3. **Lunar Client Apollo**: Protobuf-based detection of Lunar Client mods (configured in `lunar.toml`)

### Configuration Files

Located in `hackedserver-core/src/main/resources/`:
- `config.toml`: Main settings (language, debug, action delays)
- `actions.toml`: Action definitions (alerts, commands)
- `generic.toml`: Generic payload-based checks
- `forge.toml`: Forge mod detection rules
- `lunar.toml`: Lunar Client Apollo integration settings
- `languages/`: Localization files

### Key Classes

| Purpose | Location |
|---------|----------|
| Core API | `hackedserver-core/.../HackedServer.java` |
| Player state | `hackedserver-core/.../HackedPlayer.java` |
| Generic checks | `hackedserver-core/.../config/GenericCheck.java` |
| Lunar parser | `hackedserver-core/.../lunar/LunarApolloHandshakeParser.java` |
| Actions | `hackedserver-core/.../config/Action.java` |

## Dependencies

Key dependencies managed via `build.gradle.kts`:
- **apollo-protos**: Lunar Client protobuf messages (shaded and relocated)
- **protobuf**: Google Protocol Buffers (shaded and relocated)
- **adventure**: Kyori Adventure for text components
- **tomlj**: TOML configuration parsing
- **hopper**: Runtime dependency loader (for ProtocolLib)

All shaded dependencies are relocated to `org.hackedserver.shaded.*` to avoid conflicts.

## Deployment

### Prerequisites

1. Copy `~/minecraft/secrets.json` to project root (already in `.gitignore`)
2. SSH key at `~/.ssh/cursor` with access to the dedicated server

### Deploy to Production Server

```bash
# Build the plugin
./gradlew shadowJar

# Deploy (using secrets.json)
HOST="$(jq -r '.servers.test_server.ssh.host' secrets.json)"
USER="$(jq -r '.servers.test_server.ssh.user' secrets.json)"
PORT="$(jq -r '.servers.test_server.ssh.port' secrets.json)"
KEY="$(jq -r '.servers.dedicated.ssh.identity_file' secrets.json)"
PLUGINS_DIR="$(jq -r '.servers.test_server.paths.plugins_dir' secrets.json)"
JAR="build/libs/hackedserver-*.jar"

rsync -avP -e "ssh -i ${KEY/#\~/$HOME} -p ${PORT}" $JAR "${USER}@${HOST}:${PLUGINS_DIR}/"

# Restart server
UNIT="$(jq -r '.servers.test_server.systemd.unit' secrets.json)"
ssh -i "${KEY/#\~/$HOME}" -p "$PORT" "$USER@$HOST" "systemctl restart $UNIT"
```

### Server Details

- **Host**: Dedicated VPS (see `secrets.json` for IP)
- **Minecraft**: Paper/Purpur server
- **Systemd unit**: `minecraft-test.service`
- **Plugins dir**: See `secrets.json`

## Coding Conventions

- **Language**: Java 17 via Gradle toolchain
- **Text Components**: Use Adventure MiniMessage for formatted text
- **Placeholders**: Use `Placeholder.unparsed()` for raw values, `Placeholder.parsed()` only for MiniMessage content
- Keep changes focused and avoid over-engineering
