# Chess Game with Real-Time Animations & WebSocket Multiplayer

A Java chess application that supports full piece movement rules, sprite-based animations, real-time movement, and both local and networked multiplayer over WebSocket.

## Features

- **Full chess rules** for all pieces: King, Queen, Rook, Bishop, Knight, and Pawn, including blocking, captures, and two-square pawn start.
- **Real-time movement**: pieces are animated visually and the board is updated only when a piece reaches its destination.
- **Jump animation**: double-click a selected piece (or the same square) to play the `jump` sprite animation.
- **WebSocket server** with rooms, matchmaking, user authentication, ELO ratings, and disconnection handling.
- **Event bus** for internal server communication and client sound notifications.

## Project Structure

| Package     | Purpose                                                                 |
|-------------|-------------------------------------------------------------------------|
| `app`       | `SingleProcessLauncher` — starts a server and one client in one process. |
| `auth`      | `AuthManager` and `LoginCLI` for SHA-256 password auth via console.     |
| `client`    | `ClientMain` entry point for a standalone networked client.             |
| `database`  | `UserRepository` for SQLite user/ELO persistence.                       |
| `engine`    | Core gameplay: `GameEngine`, `RealTimeManager`, `GameController`, `GameSnapshot`, `StateConfigLoader`, animation state handling. |
| `image`     | `Img` utility for loading, resizing, and drawing transparent PNGs.      |
| `input`     | `BoardInputReader` and `BoardMapper` (pixel-to-board conversion).       |
| `logic`     | `MoveValidator`, `RulesEngine`, `PathValidator`, and per-piece `MoveRule` classes. |
| `model`     | `Board`, `Piece`, `Position`, `MoveEvent`, `User`.                      |
| `network`   | `ChessClient`, `Protocol` constants, `MessageHandler`, `ClientLogger`.  |
| `server`    | WebSocket server: `ChessServer`, `CommandRouter`, `Room`, `RoomManager`, `SessionManager`, `DisconnectionManager`, `EventBus`. |
| `ui`        | `HomeScreen` lobby and `GameWindow` rendering loop.                     |
| `view`      | `ImgRenderer` and `PieceImageLoader` for drawing the board and piece animations. |

## Assets

Piece sprites are organized by color, type, and state:

```
assets/pieces_mine/<color><type>/states/<state>/sprites/<frame>.png
```

For example:

```
assets/pieces_mine/wK/states/jump/sprites/1.png
```

Supported states: `idle`, `move`, `jump`, `short_rest`, `long_rest`.

`assets/board.png` is the board background.

Each `states/<state>` folder may contain a `config.json` used by `StateConfigLoader` to configure frames per second, frame count, looping, and movement speed.

## Prerequisites

- JDK 8 or later.
- The required JARs in the `lib/` folder:
  - `Java-WebSocket-1.6.0.jar`
  - `slf4j-api-2.0.18.jar`
  - `slf4j-nop-2.0.18.jar`
  - `sqlite-jdbc-3.53.2.0.jar`
- Maven is optional; the build scripts use `javac` directly with the `lib` JARs.

## Run Instructions

The project includes several Windows batch files for building and running.

### Quick single-process demo

Starts a server and one client in the same JVM:

```batch
run.bat app.SingleProcessLauncher
```

Or directly:

```batch
java -cp "target/classes;lib\*" app.SingleProcessLauncher
```

### Local game without a server

```batch
run.bat
```

Or directly:

```batch
java -cp "target/classes;lib\*" Main
```

### Dedicated server

```batch
run-server.bat
```

Or directly:

```batch
java -cp "target/classes;lib\*" server.ServerMain
```

The default WebSocket port is `8080`.

### Client

Connect to the default server on `localhost:8080`:

```batch
run-client.bat
```

Or with a custom URI:

```batch
java -cp "target/classes;lib\*" client.ClientMain ws://localhost:8080
```

### Two-player local demo

Compiles the project, starts the server, and opens two client windows:

```batch
start-all.bat
```

## How to Play

1. **Login / Register** using the console prompts.
2. **In the HomeScreen lobby:**
   - **Create Room** / **Join Room** / **List Rooms** to play with friends.
   - **Play (Matchmaking)** to be paired automatically within a +/-100 ELO range.
3. **In the game window:**
   - Click a piece to select it, then click a destination square to move.
   - Double-click the selected piece (or the same square within 300 ms) to trigger the **jump** animation.
   - Use the **Resign** or **Draw** buttons in the HomeScreen when needed.

## Notes / Known Limitations

- The server currently **relays** `MOVE` commands; move validation and game state are handled locally by each client's `GameEngine`. The server does not yet maintain a central authoritative board.
- Only `sound.effect` events from the server event bus are forwarded to clients.
- There is currently no automated test suite in the project.
