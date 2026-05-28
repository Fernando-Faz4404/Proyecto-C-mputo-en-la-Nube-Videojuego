---
tags:
  - universidad
  - proyecto
  - tank-wars
  - flujo
  - gameplay
---

# Flujo Completo del Juego — Tank Wars

> [!tip] Navegación
> [[Documentación del Cliente — ClientCloud|← Cliente]] | [[Documentación — Tank Wars|Índice]] | [[Red e Infraestructura — Tank Wars|Red →]]

---

## Fase 1: Inicio del Cliente

```mermaid
sequenceDiagram
    participant U as Usuario
    participant CL as ClientCloud.main()
    participant GW as GameWindow
    participant SM as SoundManager

    U->>CL: Ejecuta ClientCloud.jar
    CL->>GW: SwingUtilities.invokeLater(new)
    GW->>SM: loadAll() + playMenuMusic()
    GW->>GW: setContentPane(MainMenuPanel)
    GW->>U: Pantalla "TANK WARS"
```

---

## Fase 2: Menú Principal

El jugador rellena dos campos:

| Campo | Opciones |
|---|---|
| **Nombre** | Texto libre (ej. `Player247`) |
| **Número de equipos** | 2, 3, o 4 equipos |

Al pulsar **"UNIRSE A PARTIDA"**:

```mermaid
sequenceDiagram
    participant U as Jugador
    participant GW as GameWindow
    participant NC as NetworkClient
    participant LP as LobbyPanel

    U->>GW: Click "UNIRSE" (name, teamCount)
    GW->>NC: new NetworkClient(serverUrl)
    GW->>LP: new LobbyPanel(name, teamCount)
    GW->>GW: setContentPane(lobbyPanel)
    GW->>NC: connect(name, "RED", teamCount)
    NC->>NC: Lanza hilo WebSocket
```

---

## Fase 3: Conexión al Servidor

```mermaid
sequenceDiagram
    participant C as Cliente
    participant CF as Cloudflare
    participant NG as Nginx
    participant S as Servidor Java

    C->>CF: TCP + TLS handshake
    CF->>NG: Túnel → localhost:443
    NG->>S: proxy_pass :8080
    Note over C,S: WebSocket establecido

    C->>S: {"type":"JOIN", "playerId":"Player247", "teamCount":2}
    S->>S: PlayerHandler → lobby.addPlayer() → asigna equipo
    S->>C: {"type":"LOBBY_STATE", "status":"WAITING", "players":[...]}
```

---

## Fase 4: Sala de Espera (Lobby)

### Estado WAITING

El `LobbyPanel` muestra los equipos con sus jugadores:

```
┌─────────────┐  ┌─────────────┐
│   ROJO      │  │   AZUL      │
│ ► Player247 │  │  [vacio]    │
│  [vacio]    │  │  [vacio]    │
└─────────────┘  └─────────────┘

0 / 4 jugadores — Esperando...
```

### Auto-Start (Countdown)

Cuando todos los equipos tienen al menos 2 jugadores:

```mermaid
sequenceDiagram
    participant S as Servidor
    participant C as Clientes

    Note over S: Mínimo alcanzado
    S->>S: startDeadlineMs = ahora + 10s
    S->>S: Lanza hilo LobbyStartCountdown

    loop Cada segundo
        S->>C: LOBBY_STATE {status:"STARTING", countdownSeconds:N}
    end

    Note over S: Countdown = 0
    S->>S: launchGame()
```

> [!warning]
> Si un jugador se va durante el countdown y el mínimo ya no se cumple, se cancela y se vuelve a `WAITING`.

---

## Fase 5: Inicio de la Partida

```mermaid
sequenceDiagram
    participant S as Servidor
    participant C as Cliente

    S->>S: lobby.startGame() → state=IN_GAME, round=1
    S->>S: gameSeed = currentTimeMillis()
    S->>C: GAME_START {teamCount, mapResource, seed, players[]}
    C->>C: Determina equipo propio
    C->>C: new GamePanel(name, team, map, seed, net)
    C->>C: gameMap.load(mapResource)
    C->>C: spawnPowerUps(seed) → 8 power-ups
    C->>C: localTank en posición de spawn
    C->>C: startGame() → hilo GameLoop 60 FPS
```

---

## Fase 6: Partida en Curso

### Bucle de Juego (cada frame ~16.6 ms)

1. **Lee teclado** → mueve/rota tanque local
2. **Verifica colisiones** con paredes y tanques remotos
3. **Dispara** si ESPACIO + cooldown ok
4. **Envía mensajes al servidor** (MOVE, SHOOT, DEATH, POWERUP_COLLECTED)
5. **Procesa mensajes** sobre los demás jugadores

### Flujo de un Disparo

```mermaid
sequenceDiagram
    participant J as Jugador Local
    participant GP as GamePanel
    participant S as Servidor
    participant O as Otros Clientes

    J->>GP: Presiona ESPACIO
    GP->>GP: localTank.shoot() → crea Bullet
    GP->>GP: SoundManager.play("shoot")
    GP->>S: SHOOT {x, y, angle, team}
    S->>O: broadcastOthers(SHOOT)
    O->>O: onRemoteBullet() → bullets.add(new Bullet)
```

### Flujo de un Impacto y Muerte

```mermaid
sequenceDiagram
    participant B as Bala enemiga
    participant T as Tanque local
    participant S as Servidor
    participant O as Otros

    B->>T: Colisión detectada
    T->>T: takeDamage(25) → health=75
    T->>S: MOVE {..., health:75}

    Note over T: Siguiente impacto → HP = 0
    T->>T: alive=false, explosión
    T->>S: DEATH {playerId}
    S->>S: incrementOpponentScore()
    S->>O: broadcastAll(SCORE_UPDATE)
    S->>O: broadcastOthers(DEATH)
    S->>S: checkRoundEnd()
```

### Reaparición

> [!info]
> El jugador muerto puede reaparecer hasta **2 veces por ronda** pulsando **R**.

---

## Fase 7: Power-Ups

### Generación (Inicio de Ronda)

```java
Random rng = new Random(gameSeed);
// Encuentra celdas libres → mezcla con rng (determinista)
// Toma las primeras 8 → asigna tipos balanceados
// Todos los clientes obtienen los mismos 8 items
```

### Ciclo de Recolección y Respawn

```mermaid
flowchart TD
    A[Tanque pasa sobre power-up] --> B[applyPowerUp]
    B --> C[powerUp.collect - transparente]
    C --> D[Enviar POWERUP_COLLECTED]
    D --> E[Servidor: broadcastOthers]
    E --> F{¿5 recogidos en batch?}
    F -->|No| G[Esperar más]
    F -->|Sí| H[Servidor: POWERUP_RESPAWN]
    H --> I[Todos: genera 5 nuevos power-ups<br/>seed2 = gameSeed + batch * 31337]
```

---

## Fase 8: Fin de Ronda

```mermaid
sequenceDiagram
    participant S as Servidor
    participant C as Clientes

    Note over S: Todos muertos de un equipo<br/>o condición de fin

    S->>C: ROUND_END {roundNumber, totalRounds, roundWinner, redWins...}
    C->>C: Overlay "Ronda X terminada"<br/>Equipo ganador + puntos

    alt Quedan rondas
        S->>S: nextRound() → nuevo mapa + reset
        S->>C: ROUND_START {roundNumber, mapResource, seed}
        C->>C: Cargar nuevo mapa + respawn todos
    else Última ronda
        S->>C: Pantalla final de resultados
        C->>C: "ENTER para volver al menú"
        S->>S: forceDisconnectAll() → lobby.reset()
    end
```

---

## Resumen del Ciclo de Vida Completo

```mermaid
stateDiagram-v2
    [*] --> MenuPrincipal
    MenuPrincipal --> Lobby : Unirse
    Lobby --> Partida : GAME_START
    Partida --> FinRonda : Todos muertos en un equipo
    FinRonda --> Partida : ROUND_START (siguiente ronda)
    FinRonda --> Resultados : Última ronda
    Resultados --> MenuPrincipal : ENTER
```

---

> [!tip] Navegación
> [[Documentación del Cliente — ClientCloud|← Cliente]] | [[Documentación — Tank Wars|Índice]] | [[Red e Infraestructura — Tank Wars|Red →]]
