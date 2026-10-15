<p align="center">
  <a href="https://hackedserver.org/">
    <img src="https://hackedserver.org/logo.png" alt="HackedServer Logo" width="200">
  </a>
</p>

<h1 align="center">HackedServer</h1>

<p align="center">
  <strong>Detect hacked clients, unauthorized mods, and cheats on your Minecraft server</strong>
</p>

<p align="center">
  <a href="https://hackedserver.org/">Website</a> •
  <a href="https://docs.hackedserver.org/">Documentation</a> •
  <a href="https://www.spigotmc.org/resources/46485/">Spigot</a> •
  <a href="https://polymart.org/product/630/hackedserver">Polymart</a>
</p>

---

## About

HackedServer is a premium Minecraft plugin that uses packet-based analysis to detect hacked clients and unauthorized mods when players connect to your server. Protect your community from cheaters with instant detection and automated responses.

## Features

- **Client Detection** - Identifies dozens of major hacked clients including Fabric, Forge, Lunar, WorldDownloader, and Vape variants
- **Mod Intelligence** - Optional fake Forge handshake to read and flag blacklisted mods (ForgeWurst, ForgeHax, Wurst)
- **Automated Response** - Instant alerts, optional auto-bans, and configurable custom commands
- **Staff Tools** - In-game commands and inventory-style GUI for reviewing detections
- **Lightweight** - Minimal CPU/TPS impact with connection-time checks only

## Supported Platforms

| Platform | Versions |
|----------|----------|
| Paper, Purpur, Spigot, CraftBukkit | 1.16 - 1.21.11, 26.1 - 26.3 |
| Folia | 1.20+ (tested on 26.2) |
| BungeeCord | 1.16 - 1.21.x |
| Velocity | 3.x (tested on 3.5.1) |

## Requirements

- **Java 17** or higher (the server itself needs Java 21 for 1.20.5 - 1.21.x and Java 25 for 26.x)
- A packet library on Spigot/Paper/Folia, automatically downloaded if missing:
  - **ProtocolLib** 5.3+ (default up to 1.21.x)
  - or **PacketEvents** 2.14.0+ (default on 26.x and hybrid servers such as Arclight)
- **PacketEvents** on Velocity and BungeeCord (2.14.0+ for 26.x clients)
- Sign translation probing requires Paper/Folia 1.20+ and works with either packet library

## Installation

1. Download the latest release from [Spigot](https://www.spigotmc.org/resources/46485/) or [Polymart](https://polymart.org/product/630/hackedserver)
2. Place the JAR file in your server's `plugins` folder
3. Restart your server
4. Configure the plugin in `plugins/HackedServer/config.toml`

## Documentation

Full documentation is available at [docs.hackedserver.org](https://docs.hackedserver.org/)

Local docs:
- [Lunar Client configuration](docs/lunar-config.md)

## License

This project is licensed under the [HackedServer License](LICENSE.md) - see the license file for details.

---

<p align="center">
  <sub>Created by <a href="https://thomas.md">Th0rgal</a> • Also check out <a href="https://oraxen.com">Oraxen</a> and <a href="https://mcserverjars.com">MCServerJars</a></sub>
</p>
