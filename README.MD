# Proyecto Tankes Unidos

Este repositorio contiene un videojuego multijugador de tanques en Java con arquitectura cliente-servidor. El cliente utiliza Swing/AWT para la interfaz gráfica y WebSocket + Gson para comunicación en formato JSON. El servidor está construido con Java-WebSocket y Gson para manejar la sala de espera, la lógica de equipos, las rondas y los eventos de juego.

## Estructura general

- `ClientCloud/` - módulo cliente del juego.
- `TankesitosServer/` - módulo servidor del juego.

## Módulo cliente: `ClientCloud`

### Descripción

El cliente es una aplicación de escritorio con ventana Swing que ofrece:

- Menú principal y modo de prueba local sin servidor.
- Sala de espera con asignación automática de equipos.
- Juego 2D con tanques, balas, explosiones y power-ups.
- Renderizado de mapas desde archivos `.txt`.
- Audio para música de fondo, disparos, impactos y power-ups.

### Paquetes principales

- `client.ClientCloud` - punto de entrada.
- `client.game` - lógica del juego y UI.
  - `GameWindow` - gestiona la ventana principal y el flujo entre menú, lobby y juego.
  - `LobbyPanel` - muestra estado de sala de espera y jugadores por equipo.
  - `GamePanel` - bucle principal de juego, actualización de físicas, disparos, power-ups y renderizado.
  - `GameMap` - carga mapas desde recursos `src/main/resources/maps/`.
  - `HUD` - dibuja información de vida, puntuaciones y estado de ronda.
  - `KeyHandler` - controla movimiento, disparo, reaparición y cambio de ronda en modo test.
  - `AssetLoader` - carga sprites y recursos gráficos.
  - `SoundManager` - reproduce efectos y música.
- `client.entity` - entidades de juego.
  - `Tank`, `Bullet`, `Explosion`, `PowerUp`, `Team`.
- `client.net` - comunicación de red.
  - `NetworkClient` - cliente WebSocket.
  - `GameMessage` / `MessageType` - mensajes JSON que se envían y reciben.

### Recursos del cliente

- `ClientCloud/src/main/resources/maps/` - mapas de juego en texto.
- `ClientCloud/src/main/resources/sonidosTank/` - efectos de sonido.
- `ClientCloud/src/main/resources/Sprites/` - sprites de tanques, balas y explosiones.
- `ClientCloud/src/main/resources/tiles/` - texturas de terreno.

### Construcción y ejecución

1. Abrir terminal en `ClientCloud/`.
2. Ejecutar:

```bash
mvn package
```

3. Ejecutar el cliente usando el JAR generado:

```bash
java -jar target/ClientCloud-1.0-SNAPSHOT.jar
```

4. Para ejecutar en modo de desarrollo sin empaquetar:

```bash
mvn exec:java -Dexec.mainClass=client.ClientCloud
```

### Configuración del servidor remoto

El cliente utiliza por defecto la URL del servidor definida en `GameWindow`:

- `wss://game.leozamarron.dev`

También puede leerse desde un archivo `.env` en la raíz de ejecución con la clave:

```text
SERVER_URL=wss://mi-servidor-ejemplo.dev
```

### Controles del juego

- `W` / `S` - avanzar / retroceder.
- `A` / `D` - girar el tanque.
- `SPACE` - disparar.
- `R` - reaparecer si estás muerto y tienes reapariciones.
- `N` - avanzar ronda en modo de prueba local.
- `ENTER` - volver al menú cuando termine la partida.

## Módulo servidor: `TankesitosServer`

### Descripción

El servidor gestiona conexiones WebSocket, la sala de espera, la asignación de equipos, el estado de ronda y la distribución de eventos a los demás clientes.

### Paquetes principales

- `main.TankesitosServer` - punto de entrada del servidor.
- `main.WsServer` - servidor WebSocket que escucha en el puerto 8080.
- `main.PlayerHandler` - lógica de mensajes entrantes, envío a otros clientes y administración de eventos del juego.
- `main.GameLobby` - controla estado de sala, equipos, rondas y condiciones de inicio.
- `json.JSON_GameMessage` - formato de datos JSON usado para el protocolo.

### Comportamiento del servidor

- Acepta conexiones WebSocket en el puerto `8080`.
- Recibe un primer mensaje `JOIN` para registrar jugador y asignar equipo.
- Publica mensajes de estado de lobby (`LOBBY_STATE`) a todos los clientes conectados.
- Inicia la partida cuando cada equipo tiene al menos 2 jugadores.
- Envía `GAME_START`, `ROUND_START`, `ROUND_END` y actualizaciones de puntuación (`SCORE_UPDATE`).
- Reenvía mensajes de movimiento, disparo y muerte a los otros jugadores.
- Controla reaparición de power-ups y respawn en lotes.

### Construcción y ejecución

1. Abrir terminal en `TankesitosServer/`.
2. Ejecutar:

```bash
mvn package
```

3. Ejecutar el servidor:

```bash
java -jar target/TankesitosServer-1.0.jar
```

> Nota: el servidor utiliza Java 11 en su `pom.xml`, mientras que el cliente está configurado para Java 18.

## Protocolo de comunicación JSON

El cliente y el servidor usan Gson para serializar mensajes JSON de tipo `GameMessage`/`JSON_GameMessage`.

### Tipos de mensaje principales

- `JOIN` - solicitud de unión al lobby.
- `LOBBY_STATE` - estado actual de la sala de espera.
- `GAME_START` - inicio de la partida.
- `MOVE` - actualización de posición y ángulo del tanque.
- `SHOOT` - notificación de disparo.
- `DEATH` - notificación de muerte.
- `SCORE_UPDATE` - actualización de puntuación.
- `POWERUP_COLLECTED` - colecta de power-up.
- `POWERUP_RESPAWN` - reaparición de un lote de power-ups.
- `ROUND_END` / `ROUND_START` - fin y comienzo de ronda.
- `DISCONNECT` - desconexión de jugador.

### Campos comunes

- `playerId` - identificador del jugador.
- `team` - equipo asignado (`RED`, `BLUE`, `GREEN`, `YELLOW`).
- `x`, `y`, `angle` - posición y orientación del tanque.
- `health`, `alive` - estado de vida.
- `teamCount` - número de equipos habilitados.
- `mapResource` - ruta del mapa a cargar.
- `seed` - semilla usada para posicionar power-ups.

## Arquitectura general

### Cliente

1. `ClientCloud.main()` arranca `GameWindow`.
2. `GameWindow` muestra primero el menú principal.
3. Si el usuario se une a un servidor, crea `NetworkClient` y conecta por WebSocket.
4. `LobbyPanel` muestra el estado del lobby y los jugadores por equipo.
5. Cuando llega `GAME_START`, `GameWindow` crea `GamePanel`.
6. `GamePanel` ejecuta el bucle de juego, procesa entrada, actualiza físicas, colisiones y dibuja el mundo.
7. El cliente envía eventos `MOVE`, `SHOOT`, `DEATH` y power-ups al servidor.

### Servidor

1. `TankesitosServer.main()` arranca `WsServer` en el puerto 8080.
2. `WsServer` recibe conexiones y delega mensajes a `PlayerHandler`.
3. `PlayerHandler` valida el `JOIN`, registra jugadores y arma el lobby.
4. Cuando el lobby está listo, lanza la partida y gestiona rondas.
5. El servidor reenvía eventos relevantes a los otros clientes para mantener el estado sincronizado.

## Notas importantes

- El cliente tiene un modo de prueba local (`testMode`) que no requiere servidor y sirve para depuración.
- El servidor usa `CopyOnWriteArrayList` y `ConcurrentHashMap` para manejar concurrencia entre conexiones.
- Los mapas se cargan desde archivos de texto y se renderizan con tiles, por lo que es fácil agregar nuevos mapas.
- Si deseas cambiar el servidor, modifica `SERVER_URL` en un archivo `.env` o actualiza el valor en `GameWindow`.

## Recomendaciones para desarrollo

- Mantén los recursos de imágenes y sonido en `ClientCloud/src/main/resources/`.
- Para añadir nuevas reglas de juego, actualiza primero los mensajes JSON en `client.net.GameMessage` y `json.JSON_GameMessage`.
- El servidor decide los eventos de fin de ronda y reaparición de power-ups; el cliente sólo reproduce la lógica local y la visualización.

---

Este README documenta el estado actual del código encontrado en el repositorio y ayuda a entender la arquitectura cliente-servidor, los mensajes JSON y el flujo de juego.
