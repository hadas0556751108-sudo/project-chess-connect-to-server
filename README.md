# Chess Game – Real-Time Multiplayer with a Microservices Backend

A Java chess application with sprite-based animations, real-time piece movement, and online multiplayer over WebSocket. The project started as a local desktop game and grew into a containerized, multi-service system with authentication, ELO-based matchmaking, and a server-side game state.

## Highlights

- **Full chess rules** for all pieces (King, Queen, Rook, Bishop, Knight, Pawn), including blocking, captures, and the two-square pawn start.
- **Real-time movement**: pieces are animated and the board updates only when a piece reaches its destination.
- **Sprite animations** per piece state: `idle`, `move`, `jump`, `short_rest`, `long_rest` (configurable via `config.json`).
- **Microservices architecture**: separate Auth, Matchmaking, and Game services, each with its own Dockerfile, behind an Nginx gateway.
- **Authentication**: JWT tokens and BCrypt password hashing.
- **ELO matchmaking**: players are paired within a ±100 ELO range using a Redis Sorted Set, with a 60-second timeout.
- **Server-side move validation**: the game server applies moves with the same rules engine used by the client (`GameState`).
- **Disconnection handling**, rooms, and an internal event bus.
- **Unit tests** (JUnit 5 + Mockito) with JaCoCo coverage reports.

## Architecture

```
Clients (Swing UI, WebSocket)
          |
      [ Nginx ]            :9000
   /auth    /match    /game
     |         |         |
 Auth      Matchmaking   Game Server
 Service   Service       (rooms, move validation, ELO updates)
 (JWT)     (Redis queue)
     |         |         |
  MongoDB    Redis    Redis + MongoDB
```

| Service | Port | Responsibility | Storage |
|---|---|---|---|
| `auth-service` | 8081 | Registration, login, JWT issuing | MongoDB |
| `matchmaking-service` | 8082 | Queue players by ELO, create matches | Redis |
| `game-server` | 8083 | WebSocket rooms, move validation, game state, ELO updates | Redis, MongoDB |
| `nginx` | 9000 | Reverse proxy / gateway for all services | – |

A monitoring stack (Prometheus, Grafana, Jaeger) is included in the Docker Compose setup.

The design rationale (why MongoDB + Redis instead of SQLite at scale, load estimates, room distribution) is described in [`chess-server-design-dabe38.md`](chess-server-design-dabe38.md).

## Tech Stack

- **Language:** Java 8+ (Docker images use Java 11)
- **Networking:** Java-WebSocket, HTTP services
- **Databases:** MongoDB (users, ELO, history), Redis via Jedis (matchmaking queue, real-time state), SQLite (local/single-process mode)
- **Security:** JWT (`java-jwt`), BCrypt
- **UI:** Java Swing / AWT
- **Build & test:** Maven, JUnit 5, Mockito, JaCoCo
- **DevOps:** Docker, Docker Compose, Nginx, Prometheus, Grafana, Jaeger

## Project Structure

| Package | Purpose |
|---|---|
| `app` | `SingleProcessLauncher` – starts a server and one client in one process. |
| `auth` | `AuthManager`, `AuthServiceMain`, `JwtUtil`, `LoginCLI` – authentication service and console login. |
| `matchmaking` | `MatchmakingServiceMain`, `MatchmakingScanner` – ELO-based matchmaking service. |
| `redis` | `RedisClient` – thin Redis wrapper. |
| `client` | `ClientMain` – entry point for a standalone networked client. |
| `database` | `UserRepository` – user and ELO persistence. |
| `engine` | `GameEngine`, `RealTimeManager`, `GameController`, `GameSnapshot`, `StateConfigLoader`, `ScoreManager`, `MoveHistory` – core gameplay and animation state. |
| `image` | `Img` – loading, resizing, and drawing transparent PNGs. |
| `input` | `BoardInputReader`, `BoardMapper` – pixel-to-board conversion. |
| `logic` | `MoveValidator`, `RulesEngine`, `PathValidator`, and per-piece `MoveRule` classes. |
| `model` | `Board`, `Piece`, `Position`, `MoveEvent`, `User`. |
| `network` | `ChessClient`, `MatchmakingClient`, `Protocol`, `MessageHandler`, `ClientLogger`. |
| `server` | `ChessServer`, `CommandRouter`, `Room`, `RoomManager`, `SessionManager`, `DisconnectionManager`, `MatchmakingManager`, `EloManager`, `GameState`, `EventBus`. |
| `ui` | `HomeScreen` lobby, `GameWindow` rendering loop, `SoundPlayer`, `SpriteManager`. |
| `view` | `ImgRenderer`, `PieceImageLoader` – board and piece rendering. |

## Assets

Piece sprites are organized by color, type, and state:

```
assets/pieces_mine/<color><type>/states/<state>/sprites/<frame>.png
```

Example: `assets/pieces_mine/wK/states/jump/sprites/1.png`.
`assets/board.png` is the board background. Each state folder may contain a `config.json` (frames per second, frame count, looping, movement speed) read by `StateConfigLoader`.

## Prerequisites

- JDK 8 or later
- Docker Desktop (for the full microservices setup)
- Maven (optional; the batch scripts compile with `javac` using the JARs in `lib/`)

## Running the Project

### Full system with Docker Compose

```bash
docker compose up --build
```

This starts Nginx, the Auth, Matchmaking, and Game services, MongoDB, Redis, and the monitoring stack. The gateway is available at `http://localhost:9000`.

> The JWT secret and Grafana credentials in `docker-compose.yml` are development defaults and must be changed for any real deployment.

### Quick single-process demo (Windows)

Starts a server and one client in the same JVM:

```batch
run.bat app.SingleProcessLauncher
```

### Local game without a server

```batch
run.bat
```

### Dedicated server and client (Windows)

```batch
run-server.bat
run-client.bat
```

Or with a custom URI:

```batch
java -cp "target/classes;lib\*" client.ClientMain ws://localhost:8080
```

### Two-player local demo

```batch
start-all.bat
```

### Tests

```bash
mvn test
```

The coverage report is generated by JaCoCo in `target/site/jacoco`.

## How to Play

1. **Log in / register** with the console prompts.
2. **In the lobby (`HomeScreen`):**
   - **Create Room / Join Room / List Rooms** to play with friends.
   - **Play (Matchmaking)** to be paired automatically within ±100 ELO.
3. **In the game window:**
   - Click a piece, then a destination square to move.
   - Double-click the selected piece (within 300 ms) to play the **jump** animation.
   - Use **Resign** or **Draw** from the lobby when needed.

## Known Limitations / Future Work

- Only `sound.effect` events from the server event bus are forwarded to clients.
- Service-to-service communication is currently simple HTTP/Redis; a message broker (NATS/Kafka) is planned for higher scale.
- Secrets are set through environment variables with development defaults; a secrets manager should be used in production.
- Test coverage focuses on the server, auth, and networking layers; the engine and UI are not yet covered.
