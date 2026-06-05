# TDJJ-COMP3050 — Team Game Server

COMP3050 team project (2026): a Java HTTP server that tracks the state of a 2D tile-based virtual world. A web client (supplied by teaching staff) talks to the server via the **API v3** REST spec — our job is the server side: build, test, secure, deploy.

## Team Roles

| Team member | Main responsibilities |
|---|---|
| Thomas | CI/CD pipeline, AWS/Terraform infrastructure, Docker Compose deployment, monitoring stack integration, infoHandler, worldRegistry, playerState, SessionManager |
| Daniel | Grafana, Loki, Prometheus, Promtail, Ansible, TileMap, Player, PlaceHandler, Server|
| Jing | All test files, moveHandler, useHandler|
| Jayden | Docker, Kubernetes, Login, Logout, PlayerRegistry |


## AI Usage Disclosure

AI assistance was used as a development support tool during the project. It was mainly used to clarify how the server files fit together, explain request flow between handlers, sessions, world state, and map state, and help summarise the project structure for documentation and presentation purposes.

AI was also used to help reason through deployment and infrastructure problems, such as Docker Compose configuration, GitHub Actions deployment steps, AWS/Terraform security group behaviour, Ansible errors, Docker image naming, and port conflicts on the EC2 instance. In these cases, we still reviewed the suggestions, ran the commands, checked the output, and made the final implementation decisions.

## Tech Stack

- **Language/runtime:** Java 17
- **HTTP server:** Java built-in `com.sun.net.httpserver.HttpServer`
- **Build/test:** Maven and JUnit 5
- **Containerisation:** Docker and Docker Compose
- **CI/CD:** GitHub Actions, Docker Hub, SSH deployment to EC2
- **Infrastructure:** AWS EC2, Elastic IP, security groups, Terraform
- **Monitoring/logging:** Prometheus, Grafana, Loki, and Promtail
- **Security scanning:** Semgrep and Trivy

## Live Server

The deployed server runs on an AWS EC2 instance behind an Elastic IP. The current API base URL is:

```text
http://15.134.176.163
```

Example endpoints:

```text
POST http://15.134.176.163/login
GET  http://15.134.176.163/info?y=5&x=5
GET  http://15.134.176.163/move?dy=0&dx=1
```

The Docker Compose deployment also exposes monitoring tools:

- **Grafana:** `http://15.134.176.163:3000`
- **Prometheus:** `http://15.134.176.163:9090`

Note: the API currently serves HTTP. If the frontend is loaded over HTTPS, browsers may block HTTP API calls as mixed content unless the backend is also served over HTTPS.

## API Endpoints (v3)

| Endpoint | Method | Description | Success | Failure |
|---|---|---|---|---|
| `/login` | POST | JSON body `{name, encpswrd}` → session token | 200 + `{"session": "..."}` | 400 missing fields, 401 bad credentials |
| `/logout?session=` | GET | Revoke the session token | 200 | 401 invalid token |
| `/move?dy=&dx=&session=` | GET | Move one space N/S/E/W (relative to player) | 200 + `{"y", "x"}` (new absolute position) | 204 blocked/diagonal/too far, 401 |
| `/info?y=&x=&session=` | GET | 11×11 tile grid around the player (absolute y/x) | 200 + `{y, x, top, left, bottom, right, info[][]}` | 204 y/x mismatch or nothing changed, 401 |
| `/take?session=` | GET | Take the item at the player's location (0,0) | 200 | 204 no movable item, 401 |
| `/place?session=` | GET | Place an inventory item at the player's location | 200 | 204 empty inventory, 401 |
| `/use?dy=&dx=&session=` | GET | Use an adjacent (or same-cell) map element, e.g. toggle a door `D` ↔ `d` | 200 | 204 nothing usable, 401 |

Missing `dy`/`dx` default to 0. Diagonal moves and distances > 1 are invalid (204). Note: `/info` 204 on y/x mismatch is **spec-mandated** — the client resyncs from `/move`'s 200 body.

## Response Codes

- `200 OK`: request succeeded and the response body contains useful data, such as a session token, new position, or map window.
- `204 No Content`: request was valid, but there is no state change or no new data to return. Examples include blocked movement, no item to take, nothing usable nearby, or repeated `/info` polling where the map has not changed.
- `400 Bad Request`: login request is malformed, usually because required JSON fields are missing.
- `401 Unauthorized`: session token is missing, invalid, or login credentials are incorrect.
- `405 Method Not Allowed`: endpoint was called with the wrong HTTP method, such as `GET /login`.

## Authentication & Sessions

- Client sends `encpswrd` = SHA-256 hex of `"name;password"` (e.g. `"Baelin;Nice day for fishing."`). The server never decrypts — it compares against stored hashes (constant-time).
- Names are ASCII letters and hyphens only, case-sensitive.
- On success the server issues an unpredictable session token, required by every other endpoint; invalid/missing tokens → 401. Logout revokes the token server-side.
- Accounts live in `src/main/resources/players.txt` (`name;encpswrd;avatar` — hashes only), plus an `APP_USER`/`APP_PASS` env-var fallback for v2-style single-user deploys.

## Map & Movement

- 20×20 world map loaded from `src/main/resources/map.txt` at startup (space-separated tile strings — no database needed).
- Tile strings stack multiple kinds: ground first, then items/doors, player avatar digit last — `"gk2"` = grass + key + player 2.
- Blocking tiles: `B` brick wall, `S` stone wall, `W` water, `D` closed door, and other players' avatars. Walkable: `g` grass, `_` dirt, `w` wooden boards, `d` open door, `b` bridge, `f` flagstones, `t` tree, `s` sand, `p` pebbles, `. , : ;` rocks.
- Movement is one space in a cardinal direction (`dy=-1` N, `dy=+1` S, `dx=-1` W, `dx=+1` E). *Adjacent* = one square N/W/S/E, no diagonals — applies to both MOVE and USE.

## Items (TAKE / PLACE)

| Symbol | Item | Class |
|---|---|---|
| `a` | axe | tool |
| `c` | cyan potion | drink |
| `h` | heart potion | drink |
| `k` | key | artifact |

Items in the same class are mutually exclusive in the inventory: taking a heart potion while holding a cyan potion **swaps** them (the cyan potion is placed on the ground).

## Multiplayer

- Up to 10 players, each with a fixed avatar digit `0`–`9` assigned in `players.txt`.
- Per-player position and inventory (`PlayerState` via `WorldRegistry`); avatars are overlaid on the shared map and players block each other's movement — one avatar per tile.
- The client polls `/info` to see other players' movement and map changes; the server may answer 204 when nothing changed to save bandwidth.

## Run Locally & Testing

```bash
# tests (50 unit tests, 12 classes — must pass before merging)
mvn test

# run the server on :8000
export APP_USER=testuser APP_PASS=testpass
mvn compile exec:java
```

Or with Docker: `docker compose up` (nginx on :80 in front of the server on :8000).

Testing is mainly done with JUnit 5 under `src/test/java`. The tests cover server handlers, login/logout, movement rules, `/info` behaviour, player state, world registry behaviour, and map/game state logic. GitHub Actions runs the test suite on every push/PR before Docker image build and deployment checks.

## Project Structure

```
src/main/java/comp3050/
├── TileMap.java                 # map.txt loader, tile-string parsing, blocking rules
└── server/
    ├── Server.java              # entry point — HTTP server on :8000, registers handlers
    ├── LoginHandler.java        # POST /login — SHA-256 auth, issues session token
    ├── LogoutHandler.java       # /logout — revokes session
    ├── MoveHandler.java         # /move — relative move, blocking checks
    ├── InfoHandler.java         # /info — 11×11 view window
    ├── TakeHandler.java         # /take — item pickup, same-class swap
    ├── PlaceHandler.java        # /place — item drop
    ├── UseHandler.java          # /use — door toggle D ↔ d
    ├── PlayerRegistry.java      # credential store (players.txt + env fallback)
    ├── SessionManager.java      # token → player session map
    ├── WorldRegistry.java       # live per-player world state (position, inventory)
    └── PlayerState.java         # player position, avatar, inventory
src/main/resources/              # map.txt, players.txt
src/test/java/                   # JUnit 5 tests
```

## Server Code Overview

The server starts in `Server.java`. It creates one shared `TileMap`, starts Java's built-in HTTP server on port `8000`, and registers each API path with a dedicated handler class. The handlers are deliberately small entry points: each one validates the request, checks the session token if needed, calls the shared map/world helpers, and writes the correct HTTP response.

Authentication is separate from live world state. `PlayerRegistry` is the credential store: it loads accounts from `players.txt`, supports the `APP_USER`/`APP_PASS` environment fallback, and compares the submitted encrypted password with the stored hash. `LoginHandler` uses `PlayerRegistry` to validate a login request, then asks `SessionManager` to issue a random session token. Gameplay handlers use `SessionManager` to turn that token back into a username, returning `401` if the token is missing or invalid.

The current multiplayer world is stored in `WorldRegistry` and `PlayerState`. `WorldRegistry` maps usernames to their live `PlayerState`, assigns avatar digits, tracks which player occupies each tile, and provides `tryMove` so two players cannot move into the same square at the same time. `PlayerState` is the per-player record: username, avatar digit, current `y`/`x`, and inventory.

`TileMap` owns the shared map loaded from `map.txt`. The base terrain stays in a grid, while dynamic overlays such as items and door states are stored separately. This lets the handlers update items and doors without rewriting the base map. `TileMap` also centralises map rules such as horizontal wrapping, blocking checks, item pickup/drop, and door toggling.

`InfoHandler` is the main read endpoint. It authenticates the session, gets the player's authoritative position from `WorldRegistry`, builds an 11x11 window around that position, reads each cell from `TileMap`, and appends live player avatar digits from `WorldRegistry`. It also remembers the last response body per session token so repeated `/info` requests can return `204` when nothing changed. If a client sends stale coordinates, `/info` sends a fresh `200` response with the server's real position so the client can resynchronise.

The gameplay handlers update the same shared state:

1. `/login` validates credentials, retires any stale player record, creates a session, and creates the player's live world record.
2. `/move` validates `dy`/`dx`, checks `TileMap` blocking rules, then asks `WorldRegistry.tryMove` to update the player's position safely.
3. `/info` returns the visible map window centred on the server-side player position.
4. `/take` removes an item from the current map cell and adds it to the player's inventory, including same-class item swapping.
5. `/place` removes an item from the player's inventory and writes it back to the `TileMap` overlay.
6. `/use` applies interactions such as toggling doors between closed `D` and open `d`.
7. `/logout` revokes the token and retires the player from the world, dropping held items back onto the map.

## CI/CD Pipeline

- **`ci.yml`** runs on code changes to validate the project. It runs Maven tests, builds the Docker image, runs Semgrep static analysis, and scans the image with Trivy.
- **`deployment.yml`** runs on pushes to `main`. It builds and pushes `game-server:latest` to Docker Hub, scans the image, SSHes into EC2, pulls the latest repository state, writes `.env` from GitHub Secrets, pulls Docker Compose images, removes the old standalone `app` container if it exists, and starts the full Compose stack.
- **Docker Hub** stores the production app image so EC2 only pulls and runs a prebuilt image instead of compiling Java code during deployment.
- **Docker Compose** starts the app, Nginx, Prometheus, Grafana, Loki, and Promtail as one deployment unit.

## Security

- Passwords are never stored or compared in plaintext. The client submits `encpswrd`, and the server compares it with stored SHA-256 hashes.
- Session tokens are random UUID-based strings with hyphens removed, and protected endpoints return `401` for missing or invalid tokens.
- Secrets are kept out of source control. Runtime credentials, Docker Hub credentials, and EC2 SSH keys are supplied through GitHub Secrets, `.env`, or ignored local files.
- `.gitignore` excludes `.env`, Terraform state, private keys, compiled classes, and other local/generated files.
- Semgrep checks the source for common security issues, and Trivy scans Docker images for high/critical vulnerabilities.

## Infrastructure (Terraform)

`terraform/main.tf` provisions AWS **ap-southeast-2** infrastructure:

- a `t3.micro` EC2 instance using Amazon Linux 2023
- Docker installation and startup through EC2 user data
- an Elastic IP so the server has a stable public address
- a security group for SSH, HTTP, app access, Grafana, and Prometheus

The Terraform variables live in `terraform/terraform.tfvars` locally:

```bash
terraform init
terraform apply -var="key_pair_name=YOUR_KEY" -var="dockerhub_username=YOUR_USER"
```

## Branch Workflow

- `main` is the deployment branch. Pushing to `main` triggers the GitHub Actions deployment workflow.
- Feature work should be done on a separate branch, then merged into `main` after tests pass.
- Before pushing, run `mvn test` locally when possible and check `git status` so generated files such as `.DS_Store`, `.env`, private keys, and Terraform state are not committed.
- If `main` has moved ahead on GitHub, pull/rebase before pushing so local deployment changes are applied on top of the latest team work.

## Key Design Decisions

- **One handler per endpoint:** each API endpoint has its own handler class, keeping request parsing and response logic easy to follow.
- **Shared map, separate live state:** `TileMap` stores terrain, items, and doors, while `WorldRegistry` stores live player position/inventory. This avoids mixing permanent map data with temporary session state.
- **Authoritative server state:** `/move` and `/info` use the server's stored player position instead of trusting the client completely.
- **Session-based authentication:** login creates a token, and gameplay endpoints validate that token before reading or changing world state.
- **204 for unchanged/no-op responses:** blocked moves, unchanged `/info` polling, and unavailable item/use actions return `204` to match the API style and avoid sending unnecessary bodies.
- **Prebuilt Docker images in production:** EC2 pulls the app image from Docker Hub instead of building with Maven on the server.
- **Compose-based deployment:** the app, reverse proxy, and monitoring tools are deployed together so the runtime environment is reproducible.