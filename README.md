# TDJJ-COMP3050
Welcome to our project

Tech-Stack:
Java <br />
Map stored via text file into memory <br />
built-in Java HTTP server <br />
Docker for containerisation <br />

Basic project plan:
• The server maintains a tile-based map and tracks a player character’s position <br />
• Two API endpoints: <br />
– /move?dy=DY&dx=DX — move the character one space (N/S/E/W) <br />
– /info?y=Y&x=X — return map tile data around a location <br />
• The map is a grid of tile types (grass, walls, water, doors, etc.) — some tiles block movement <br />
• The client displays an 11×11 view centred on the player <br />
• The server stores the map as a text file loaded into memory <br />
