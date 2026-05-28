# README.md

## Project Overview

2D multiplayer team-based tank game client. Java Swing/AWT for rendering, Gson for network serialization. Connects to server at `192.168.100.18:2555`. Can run offline for local testing.

## Architecture

- **`ClientCloud`** — Entry point. Reads `SERVER_IP`/`SERVER_PORT` from `.env` file, parses args (`playerName`, `team`, `offline`, `mapPath`) and launches `GameWindow`.
- **`game/AssetLoader`** — Singleton that loads all sprites, tile images (from `/tiles/*.png`, `/sprites/*.png`), and map files (from `/maps/*.txt`). Generates colored placeholders when PNGs are missing.
- **`game/GamePanel`** — Main game loop (60 FPS). `JPanel` + `Runnable`. Update → repaint cycle with collision, bullets, explosions.
- **`game/GameWindow`** — `JFrame` wrapper. Wires up panel, optional server connection, starts game.
- **`game/GameMap`** — Loads tile grid from `.txt` files via `AssetLoader.loadMap()`. Renders tiles as images. Provides `isSolid()` and `getWallsNear()` for collision.
- **`game/KeyHandler`** — `KeyListener`: WASD movement, SPACE shoot, R respawn.
- **`game/HUD`** — Draws team scores, player info, controls hint, death overlay.
- **`entity/Tank`** — Position, angle, health, team. Draws via sprite images + `AffineTransform` rotation.
- **`entity/Bullet`** — Projectile with velocity, lifetime, sprite-based rendering.
- **`entity/Explosion`** — 5-frame sprite animation.
- **`entity/Team`** — Enum `RED`/`BLUE` with display colors.
- **`net/NetworkClient`** — Socket client using `DataOutputStream.writeUTF()` / `DataInputStream.readUTF()` + Gson JSON.
- **`net/GameMessage`** — JSON message with factory methods. **`net/MessageType`** — Enum: JOIN, MOVE, SHOOT, DEATH, DISCONNECT, STATE_UPDATE, SCORE_UPDATE.

## Resource Layout

```
src/main/resources/
├── maps/map01.txt              # Space-separated tile IDs (0=grass 1=wall 2=water 3=sand)
├── sprites/{tank_red,tank_blue,barrel,bullet_red,bullet_blue}.png
├── sprites/explosion/exp_{0..4}.png
└── tiles/{grass,wall,water,sand}.png
```

All PNGs are optional — `AssetLoader` generates colored placeholders if missing.

## Key Conventions

- **Assets**: All loaded through `AssetLoader.get()`. Call `.load()` once at startup. Maps are `.txt` files, sprites/tiles are PNGs.
- **Serialization**: `DataOutputStream.writeUTF()` / `DataInputStream.readUTF()` with Gson JSON strings.
- **Threading**: Game loop, network send, and network receive each get their own daemon `Thread`.
- **Packages**: `client.game` (engine), `client.entity` (game objects), `client.net` (networking).

## Build & Run

```bash
mvn compile
mvn exec:java -Dexec.mainClass=client.ClientCloud -Dexec.args="Player1 BLUE offline"
```

## Dependencies

- **Gson 2.10.1** — sole external dependency
- **Java 25** — uses switch expressions
