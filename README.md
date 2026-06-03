# TDJJ-COMP3050
Welcome to our project

Tech-Stack: <br />
Java <br />
Map stored via text file into memory <br />
built-in Java HTTP server <br />
Docker for containerisation <br />

Basic project plan: <br />
• The server maintains a tile-based map and tracks a player character’s position <br />
• Two API endpoints: <br />
– /move?dy=DY&dx=DX — move the character one space (N/S/E/W) <br />
– /info?y=Y&x=X — return map tile data around a location <br />
• The map is a grid of tile types (grass, walls, water, doors, etc.) — some tiles block movement <br />
• The client displays an 11×11 view centred on the player <br />
• The server stores the map as a text file loaded into memory <br />

# Assignment of tasks - add assignees in Github Issues <br />
• Who will set up the map file and map-loading code?
• Who will implement the /move endpoint?
• Who will implement the /info endpoint?
• Who will set up the project infrastructure (Docker, CI, testing)?

1. Build (compile + run tests):
cd ~/TDJJ-COMP3050
mvn compile        # just compile
mvn test           # compile + run tests (currently 0 JUnit tests)
mvn clean compile  # if you want a fresh build from scratch

2. Run the server (credentials come from env vars):
export APP_USER=testuser
export APP_PASS=testpass
mvn compile exec:java

The server starts on port 8000. Stop it with Ctrl+C.