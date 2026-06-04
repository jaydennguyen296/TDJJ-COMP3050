# TDJJ-COMP3050 — Team Game Server

COMP3050 team project (2026): a Java HTTP server that tracks the state of a 2D tile-based virtual world. A web client (supplied by teaching staff) talks to the server via the **API v3** REST spec — our job is the server side: build, test, secure, deploy.

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

## Run Locally

```bash
# tests (50 unit tests, 12 classes — must pass before merging)
mvn test

# run the server on :8000
export APP_USER=testuser APP_PASS=testpass
mvn compile exec:java
```

Or with Docker: `docker compose up` (nginx on :80 in front of the server on :8000).

## Project Structure

```
src/main/java/comp3050/
├── TileMap.java                 # map.txt loader, tile-string parsing, blocking rules
└── server/
    ├── Server.java              # entry point — HTTP server on :8000, registers handlers
    ├── LoginHandler.java        # POST /login — SHA-256 auth, issues session token
    ├── LogoutHandler.java       # /logout — revokes session
    ├── MoveHandler.java         # /move — relative move, blocking checks
    ├── InfoHandler.java         # /info — 11×11 view window
    ├── TakeHandler.java         # /take — item pickup, same-class swap
    ├── PlaceHandler.java        # /place — item drop
    ├── UseHandler.java          # /use — door toggle D ↔ d
    ├── PlayerRegistry.java      # credential store (players.txt + env fallback)
    ├── SessionManager.java      # token → player session map
    ├── WorldRegistry.java       # live per-player world state (position, inventory)
    └── PlayerState.java         # player position, avatar, inventory
src/main/resources/              # map.txt, players.txt
src/test/java/                   # JUnit 5 tests
```

## CI/CD & Security

- **`ci.yml`** — every push/PR: `mvn test`, Docker build, Semgrep SAST (`.semgrep.yml` flags hardcoded credentials), Trivy image scan (fails on HIGH/CRITICAL).
- **`deployment.yml`** — on push to `main`: build & push Docker image to Docker Hub (`game-server:latest`), Trivy scan, then SSH deploy to EC2 (`docker run -p 80:8000` with `APP_USER`/`APP_PASS` from GitHub Secrets).
- Multi-stage Dockerfile (Maven + Temurin 17 build → JRE-only runtime).
- Secrets never in source: GitHub Secrets for Docker Hub / EC2 / app credentials; `.gitignore` excludes `.env`, keys, Terraform state.

## Infrastructure (Terraform)

`terraform/main.tf` provisions AWS **ap-southeast-2**: a `t3.micro` EC2 instance (Amazon Linux 2023, Docker installed via user-data), a security group (ports 22/80/8000), and an Elastic IP.

```bash
terraform init
terraform apply -var="key_pair_name=YOUR_KEY" -var="dockerhub_username=YOUR_USER"
```
