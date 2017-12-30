import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import java.text.DecimalFormat; 
import java.util.Collections; 
import java.util.*; 
import java.awt.*; 
import java.util.Random; 
import java.util.ArrayList; 
import java.util.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class Tron extends PApplet {

// import processing.sound.*;





// Game created by Dan, Kabir, and Josh


/**********************************************************************************************************
 * IMPORTANT INFORMATION!                                                                                 *
 *                                                                                                        *
 * ENABLE_SOUND toggles the game sound effects. It is enabled by default, however may be set to false if  *
 * the sound effects cause the game to crash (the sound library breaks on certain computers).             *
 **********************************************************************************************************/

boolean ENABLE_SOUND = false;


ArrayList<Player> players = new ArrayList();
ArrayList<String> directions = new ArrayList();
HashMap<String, Location> spawns = new HashMap();
ArrayList<Location> grid = new ArrayList();
ArrayList<Location> gridCache = new ArrayList();

PFont f = null;
int w = 0;
int h = 0;
final int topHeight = 50;
final int pixelSize = 5;
GameState state = GameState.MENU;
ScoreBar bar = null;
boolean doRespawn = false;
boolean doFullRender = true;
boolean doLeaderboard = false;
boolean runGame = false;
float framerate = 30;
double respawnTimer = 3.0f;
double respawnTimerBackup = respawnTimer; // Need a better way to save this variable
ArrayList <PowerUp> powerUps;

// SOUND [begin]
//
// SoundFile readyToPlay;
// SoundFile powerUp;
// SoundFile gameOver;
// SoundFile preGame;
// SoundFile theGrid;
// SoundFile inGame;
// SoundFile postGame;
// SoundFile gameSound;
// SoundFX sfx;

// SOUND [end]

// Get the powerup from the specified location
public PowerUp getPowerUp(Location loc) {
  for (PowerUp pu : powerUps) {
    for (Location l : pu.imageLocs) {
      if (l.equals(loc)) {
        return pu;
      }
    }
  }
  return null;
}

// Removes the 2x2 grid of a powerup (both from the screen and from the list)
public void removePowerUp(PowerUp p) {
  for (Location loc : p.getLocations()) {
    Location replacement = new Location(loc.getX(), loc.getY());
    gridCache.add(replacement);
  }

  powerUps.remove(p);
  render();
}

// Sets up the game, defines the screen size, and sets the font
public void setup() {
  // // SOUND [begin]
  // if (ENABLE_SOUND) {
  //   sfx = new SoundFX ();
  //   readyToPlay = new SoundFile (this, "ReadyToPlay.mp3");
  //   powerUp = new SoundFile (this, "PowerUp.wav");
  //   gameOver = new SoundFile (this, "GameOver.mp3");
  //   preGame = new SoundFile (this, "PreGame.mp3");
  //   inGame = new SoundFile (this, "InGame.mp3");
  //   postGame = new SoundFile (this, "PostGame.mp3");
  //   gameSound = new SoundFile (this, sfx.chooseGameSound());
  //   sfx.preGame();
  // }
  // // SOUND [end]

  f = createFont("HelveticaNeue-Light", 60, true);
  //println(join(PFont.list(), "\n"));
  directions = new ArrayList();
  directions.add("LEFT");
  directions.add("RIGHT");
  directions.add("UP");
  directions.add("DOWN");
  
  resetGame();
}

// Resets the framerate, ColorPicker size, fields, playerlist, and game grid.
public void resetGame() {
  frameRate(framerate);
  w = width;
  h = height;
  if (width % pixelSize != 0 || height % pixelSize != 0) {
    throw new IllegalArgumentException();
  }

  this.resetGrid();
  this.doRespawn = false;
  this.runGame = true;

  spawns = new HashMap();
  spawns.put("RIGHT", new Location(50, (h - topHeight) / 2)); // LEFT SIDE
  spawns.put("LEFT", new Location(w-50, (h - topHeight) / 2)); // RIGHT SIDE
  spawns.put("DOWN", new Location(w/2, topHeight + 50)); // TOP SIDE
  spawns.put("UP", new Location(w/2, h - 50)); // BOTTOM SIDE
}

// Returns a sorted leaderboard of players (most lives left -> least)
public ArrayList<Player> getLeaderboard() {
  ArrayList<Player> result = new ArrayList(players);
  for (Player player : players) {
    Collections.sort(result);
    Collections.reverse(result);
  }
  return result;
}

// Sets framerate to 2 and displays the game over screen (via call to draw)
public void gameOver() {
  // if (ENABLE_SOUND) {
  //   sfx.endGame();
  // }
  doLeaderboard = true;
  frameRate(10);
  //this.state = GameState.MENU;
  //If line 151 runs, it bypasses the gameover leaderboard screen for some reason
  //redraw();
}

// Places walls and powerups on the grid
public void populateGrid() {
  for (int i=0; i<5; i++) {
    int chance = (int) random(10);
    if (chance <= 3) {
      int hh = ((int) random(50) + 1) * pixelSize;
      int ww = ((int) random(30) + 1) * pixelSize;

      int x = ((int) random(((width/2)/pixelSize))) * pixelSize + width/4;
      int y = ((int) random((height-topHeight)/pixelSize)) * pixelSize + topHeight;

      new Wall(w/2, 190, hh, ww).render();
    }
  }

  int fewestNumberOfPowerUps = 3;
  int greatesNumberOfPowerUps = 6;
  int wSize = 2;
  int hSize = 2;

  powerUps = new ArrayList <PowerUp> ();
  createPowerUps (fewestNumberOfPowerUps, greatesNumberOfPowerUps, wSize, hSize);
}

// Spawns a random amount of powerups on the screen
public void createPowerUps (int low, int high, int h, int w) {
  int num = (int) (random (low, high + 1));
  for (int i = 0; i < num; i++) {
    int x = ((int) random((width/pixelSize))) * pixelSize;
    int y = ((int) random((height-topHeight)/pixelSize)) * pixelSize + topHeight;
    if (getLocation(x, y).getType() == LocationType.AIR) {
      powerUps.add (new PowerUp (x, y, w, h));
    }
  }
}

// Cleans the grid and and resets grid cache
public void resetGrid() {
  background(50, 50, 50);
  this.grid = new ArrayList();
  //boolean black = true;
  for (int y=topHeight; y<h; y+=pixelSize) {
    for (int x=0; x<w; x+=pixelSize) {
      grid.add(new Location(x, y));
    }
  }
  populateGrid();

  this.gridCache = new ArrayList();
  this.doFullRender = true; // Why does this variable save as true in this method, but not when placed into resetGame()?
}

// Returns the location associated with a given x and y coordinate (or null if nonexistant)
public Location getLocation(Location loc) {
  return getLocation(loc.getX(), loc.getY());
}

public Location getLocation(int x, int y) {
  /* The initial, much slower way of fetching a location from the grid
   println();
   int c = 0;
   for (Location loc : grid) {
   if (loc.equals(new Location(x, y))) {
   println("C="+c);
   break;
   }
   c++;
   }*/


  try {
    if (x % pixelSize != 0) {
      return null;
    }

    // Jump directly to index of location

    /* PLAN:

     Original plan was to do: get (y-1) * getHeight() + x % pixelSize , but this returned numbers that were far too high (and threw index out of bounds exceptions). Thus, I returned to the planning
     phase, this time using a set of test coordinates:

     [example coords]

     x = 15
     y = 55

     c=123 [the actual index as done the slow way]

     55 - 50 = 5 [mistake was that I forgot to divide y - top by 5, and thus I was getting absurdly high values for my calculated "y" in grid

     (width / 5) * ((y - top) / 5) + x / 5

     New problem: Bike wraps around the edge when it collides [still ends the player's game, but this needs fixing]
     Solution: Just add an if statement checking if the Y changed without the player going up/down (see Player.pde -> checkCrash())

     */

    int index = (width / pixelSize) * ((y - topHeight) / pixelSize) + x / pixelSize; // A faster way to get the index of a location
    //println("D="+index);
    return grid.get(index);
  }
  catch (IndexOutOfBoundsException e) {
    //e.printStackTrace(); // An exception should be thrown for debugging purposes
  }

  return null;
}

// Draws the current game screen (and only draws new locations as opposed to drawing every location every time)
public void render() {

  /*
      gridCache is a much more efficient way of rendering the grid -- instead of iterating every single location with each render(),
   it only draws the locations which have changed, cutting down on lag.
   */

  ArrayList<Location> queue = gridCache;

  if (doFullRender) { // On the first render it should draw the entire grid
    background(255, 255, 255);
    queue = grid;
    doFullRender = false;
    this.bar = new ScoreBar(players, 0, topHeight/2 + topHeight/4);
  }

  for (PowerUp p : powerUps) { // Workaround for cache being overwritten
    p.addToCache();
  }

  for (Location loc : queue) {
    if (loc.getType() != LocationType.POWERUP) {
      int c = loc.getColor();
      stroke(c);
      fill(c);

      rect(loc.getX(), loc.getY(), pixelSize-1, pixelSize-1);
    } else {
      Location l2 = getLocation(loc);
      if (l2 != null) {
        l2.setType(LocationType.POWERUP);
      }
    }
  }

  for (PowerUp p : powerUps) {
    p.render();
  }

  gridCache = new ArrayList();
}

// Moves respective player when a key is pressed
public void keyPressed() {
  for (Player player : players) {
    // Need two if statements because key = char and keyCode = int
    if (key == CODED) {
      if (player.isKey(keyCode)) {
        player.changeDirection(keyCode);
      }
    } else {
      if (player.isKey(key)) {
        player.changeDirection(key);
      }
    }
  }
}

// Draws the game screen (grid/players if it's in game, respawn screen, and game over screen)
public void playGame() {
  if (!runGame) {
    return;
  }
  if (doLeaderboard) {
    String gameOver = "Game Over";
    String leaderboard = "";

    int place = 1;
    for (Player player : getLeaderboard()) {
      leaderboard += "\n"+(place++)+". "+player.name()+" ("+player.lives()+" lives)";
    }

    background(0, 0, 0);
    PFont f2 = createFont("HelveticaNeue-Bold", 85, true);
    textAlign(CENTER);
    textFont(f2);
    fill(color(134, 244, 250));
    text(gameOver, width/2, height/2 - 50);

    textFont(f);
    text(leaderboard, width/2, height/2 -35); // Make text size a variable
    textAlign(BASELINE);
    this.doLeaderboard = false;
    this.runGame = false;
    noLoop();

    // Need a way to keep this text on the screen without it getting overwritten by setup();
    // Source for below code: https://stackoverflow.com/questions/2258066/java-run-a-function-after-a-specific-number-of-seconds
    new java.util.Timer().schedule(
      new java.util.TimerTask() {
      @Override
        public void run() {
        setup();
        this.cancel();
      }
    }
    , 2000);
  } else if (this.doRespawn) {
    if (respawnTimer > 0) {
      background(0, 0, 0);
      textFont(f, 60);
      fill(color(134, 244, 250));
      DecimalFormat df = new DecimalFormat("0.0");
      textAlign(CENTER);
      text("Restarting In\n"+df.format(respawnTimer), width/2, height/2);
      textAlign(BASELINE);
      respawnTimer -= 0.1f;
    } else {
      respawnTimer = respawnTimerBackup;
      this.resetGrid();
      int count = 0;

      int index = 0;
      for (int i=players.size()-1; i>=0; i--) {
        Player player = players.get(i);
        if (player.lives() > 0) {
          String dir = directions.get(index++); // just assume # players <= # of directions
          player.respawn(spawns.get(dir));
          player.setDirection(dir);
          count++;
        }
      }

      if (count <= 1) {
        // if (ENABLE_SOUND) {
        //   sfx.endGame();
        // }
        gameOver();
        return;
      }

      this.doRespawn = false;
      frameRate(framerate);
      return;
    }
  } else {
    int dead = 0;
    int eliminated = 0;

    // Draw the current ColorPicker
    for (Player player : players) {
      if (player.isAlive()) {
        player.move();
      } else {
        dead++;
        if (player.lives() == 0) {
          eliminated++;
        }
      }
    }

    if (players.size() - dead <= 1) {
      if (eliminated >= players.size() - 1) { // Can probably merge the two calls to setup()
        // RETURN TO MENU / PLAY AGAIN screen
        gameOver();
        return;
      }
      frameRate(10);
      doRespawn = true;
    } else {
      render();
    }
  }
  if (bar != null) {
    bar.render();
  }
}

// Displays the game's start menu, which lets player enter a number 2-4 to pick how many players to run the game with
public void startMenu() {
  textAlign(CENTER);
  textFont(f);
  background(20, 20, 20);
  PImage img = loadImage ("TRON.png");
  image (img, 10, 100, width-20, 180);
  //text("TRON", width/2, height/2);
  fill(color(109, 236, 255));
  textSize(34);
  text("Press [2-4] to Select a Game Size", width/2, height/2 + 50);

  int playerSize = -1;

  // Below code is from: https://stackoverflow.com/questions/628761/convert-a-character-digit-to-the-corresponding-integer-in-c
  if (keyPressed) {
    playerSize = key - '0';

    if (playerSize >= 2 && playerSize <= 4) {
      // Later on player 1 and player 2 will be taken from text box input (same for color)
      this.players = new ArrayList();
      ArrayList<String> controls = new ArrayList();
      controls.add("wasd"); // player 1
      controls.add("arrows"); // player 2
      controls.add("ijkl"); // player 3
      controls.add("gvbn"); // player 4

      for (int i=0; i<playerSize; i++) {
        String controlString = controls.get(i);
        char[] controlArray = new char[4];
        if (controlString.equals("arrows")) {
          controlArray[0] = UP;
          controlArray[1] = LEFT;
          controlArray[2] = DOWN;
          controlArray[3] = RIGHT;
        } else {
          for (int j=0; j<4; j++) { // Add each direction to the array using charAt
            controlArray[j] = controlString.charAt(j);
          }
        }
        this.players.add(new Player(controlArray[0], controlArray[1], controlArray[2], controlArray[3]).setControlKeys(controlString)); // One player mode breaks game
      }

      this.state = GameState.CREATE_PLAYER;
    }
  }
}

// Displays color picker screen until player has picked a color
public void pickColor(Player player, ColorPicker picker) {
  picker.setPlayer(player);
  background(0xffE3E3E3);

  if (player.getColor() != color(0, 0, 0)) {
    TextBox nameInput = new TextBox(player);
    if (keyPressed) {
      nameInput.keyPressed();
    }
    nameInput.draw();
  } else {
    picker.draw();
    picker.keyPressed();
  }

  key = 0;
}

// Player selection screen -- pick a name and color
public void createPlayer() {
  ColorPicker colorPicker = new ColorPicker();

  for (Player player : players) {
    if (player.getColor() == color(0, 0, 0) || (player.hasName() == false)) {
      pickColor(player, colorPicker);
      return;
    }
  }

  // if (ENABLE_SOUND) {
  //   sfx.moveToGame();
  // }

  state = GameState.PLAY_GAME;

  // Spawn players
  int index = 0;
  for (int i=players.size()-1; i>=0; i--) {
    String dir = directions.get(index); // Need to add players in reverse so that when they respawn, they move in the right direction
    players.get(i).setSpawn(spawns.get(dir)).setDirection(dir);
    index++;
  }

  // Update game state only if all players have name and color
}

// Display's the relevant screen (relating to the game state)
public void draw() {
  switch(state) {
  case MENU:
    background(0, 0, 0);
    startMenu();
    break;
  case CREATE_PLAYER:
    frameRate(20);
    createPlayer();
    break;
  case PLAY_GAME:
    frameRate(framerate);
    playGame();
    break;
  }
}

// Various getters/setters
public int getWidth() {
  return w;
}
public int getHeight() {
  return h;
}
public int getTopHeight() {
  return topHeight;
}
public int getPixelSize() {
  return pixelSize;
}
public PFont getFont() {
  return f;
}

public ArrayList<Player> getPlayers() { // Yes we don't actually need this getter, but it's good practice
  return players;
}

public ArrayList<Location> getGridCache() {
  return gridCache;
}

public ArrayList<Location> getGrid() {
  return this.grid;
}
class Button{
  int x,y,w,h;
  String label;
  boolean overButton;
  int col;

  //Creates a new Button
  Button(int x, int y, int w, int h, String label, int c){
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
    this.label = label;
    this.col = c;
  }

  public void draw(){
    stroke(col);   
    fill(col);
    
    rect(x,y, w,h);
    fill(0);
    textAlign(CENTER, CENTER);
    
    text(label, x+w/2, y + h/2);    //Creates a button in a retangle, filled with the color from the constructor
  }
  
  public int getCol() {      //Returns the color of the object
    return this.col; 
  }
}
class ColorPicker {
  int playerColor;
  Player currentPlayer;

  Button red;
  Button pink;
  Button blue;
  Button yellow;

  public ColorPicker() {
    playerColor = color(0,0,0);
    currentPlayer = null;
    int spacing = width / 50;
    int buttonWidth = 80;
    int buttonHeight = 40;
    red = new Button(width/2-200, height/2, buttonWidth, buttonHeight, "a", color(0xffEA5056));
    pink = new Button(width/2-100, height/2, buttonWidth, buttonHeight, "b", color(0xffF28BED));
    blue  = new Button(width/2, height/2, buttonWidth, buttonHeight, "c", color(0xff24ADF0));
    yellow = new Button(width/2+100, height/2, buttonWidth, buttonHeight, "d", color(0xffF0E924));
  }
  
  // Sets the current player for the colorpicker
  public void setPlayer(Player player) {
    this.currentPlayer = player;
  }

  public void keyPressed() {
    if(key == 'a'){
      currentPlayer.setColor(red.getCol());
    }
   if(key == 'b'){
      currentPlayer.setColor(pink.getCol());
    }
   if(key == 'c'){
      currentPlayer.setColor(blue.getCol());
    }
    if(key == 'd'){
      currentPlayer.setColor(yellow.getCol());
    }
  }
  
  
  public void draw() {
      if (currentPlayer == null) {
        throw new NullPointerException(); 
      }
      textAlign(CENTER);
      textSize(60);
      text("Please select your color", width/2, 50);
      textSize(16);
      red.draw();
      blue.draw();
      pink.draw();
      yellow.draw();
  }
}
class Location {
  
  int x;
  int y;
  int c;
  boolean isImage = false;
  LocationType type;
  
  Location(int x, int y, int c, LocationType type) {
    this.x = x;
    this.y = y;
    this.c = c;
    this.type = type;
  }
  
  Location (int x, int y, LocationType type, boolean b) {
    this.x = x;
    this.y = y;
    this.isImage = b;
    this.type = type;
  }
  
  Location(int x, int y) {
    this(x, y, color(0,0,0), LocationType.AIR);
  }
  
  public int getX() {
    return this.x; 
  }
  
  public int getY() {
    return this.y; 
  }
  
  public Location get() {
    return this; 
  }
  
  public void setImage(boolean b) {
    this.isImage = b;
  }
  
  public boolean isImage() {
    return this.isImage; 
  }
  
  public void setColor(int c) {
    this.c = c; 
  }
  
  public int getColor() {
    return this.c;
  }
  
  public void setType(LocationType type) {
    this.type = type; 
  }
  
  public LocationType getType() {
    return this.type;
  }
  
  public boolean equals(Location other) {
    return this.getX() == other.getX() && this.getY() == other.getY();
  }
  
  public String toString() {
    return "x="+x+",y="+y; 
  }
}
 //<>// //<>// //<>// //<>// //<>// //<>// //<>// //<>// //<>// //<>// //<>// //<>// //<>//



final int DEFAULT_SPEED = 1;

class Player implements Comparable
{
  private final int UPKEY, DOWNKEY, LEFTKEY, RIGHTKEY; // The directional keys
  //private final int up, down, left, right; // Ints to store the current direction
  private int col;
  private String name;
  private ArrayList<Location> playerLocations;
  private int direction;
  private int speed;
  private boolean alive;
  private boolean hasName;
  private int speedTimer;
  private String displayKeys;
  Random generator = new Random();
  int lives = 3;

  // SoundFX sfx = new SoundFX ();

  //Changed the constructor so that it did not initialize its name or color
  public Player (char u, char l, char d, char r)
  {
    // For the arrow keys it's possible to pass in an ampersand and itll read as a direction, but not sure how to fix that bug

    UPKEY = (int) u;
    DOWNKEY = (int) d;
    LEFTKEY = (int) l;
    RIGHTKEY = (int) r;
    this.name = "";
    this.hasName = false;

    this.col = color(0, 0, 0);
    playerLocations = new ArrayList<Location>();
  }

  public Player setSpawn(Location loc) {

    this.respawn(loc);
    return this;
  }
   //<>//
  // Used for the player creation screen where it displays that person's controls
  public Player setControlKeys(String keys) {
    this.displayKeys = keys;
    return this;
  }

  public String getControlKeys() {
    return this.displayKeys;
  }

  // Moves player in the specified direction //<>//
  public Player setDirection(String direction) { //<>// //<>//
    switch (direction) {
      case ("UP"):
      this.direction = UPKEY;
      break;
      case ("DOWN"):
      this.direction = DOWNKEY;
      break;
      case ("LEFT"):
      this.direction = LEFTKEY;
      break;
      case ("RIGHT"): //<>//
      this.direction = RIGHTKEY; //<>//
      break;
    }
 //<>//
    return this;
  }

  // Gets the line of locations between two points (used for when speed > 1)
  public ArrayList<Location> getLine(Location from, Location to) {
    // Add delta X and add delta Y (if vertical, delta x = 0, if horizontal, delta y = 0)
    if (to == null) {
      return new ArrayList();
    } // In the future need a way to get all the points up to the border so that it draws a complete line.

    ArrayList<Location> result = new ArrayList();
    int deltaX = (from.getX() - to.getX())/getPixelSize(); // Amount of "pixels" between two the locations (x-wise) if delta = 10, 2 pixels so increase by 5 each time //<>//
    int deltaY = (from.getY() - to.getY())/getPixelSize();

    // Ensures that the deltaX and deltaY are valid (might not need this because we already know the location is valid)
    //if (deltaX % getPixelSize() != 0 || deltaY % getPixelSize() != 0) { throw new IllegalArgumentException(); }

    int currentX = from.getX();
    int currentY = from.getY();

    // Code from: https://stackoverflow.com/questions/13988805/fastest-way-to-get-sign-in-java
    int xSign = (int) Math.signum(deltaX);
    int ySign = (int) Math.signum(deltaY);

    for (int i=0; i<Math.abs(deltaX + deltaY); i++) { // This should include "to". deltaX + deltaY will be the amount of blocks to go up/horizontal because one should always be 0.
      currentX -= xSign * getPixelSize();
      currentY -= ySign * getPixelSize();
      Location loc = getLocation(currentX, currentY);

      result.add(loc);
      if (loc == null) {
        return result;
      }
    }

    return result;
  }


  // Moves the bike forward x amount
  public void move()
  {
    if (!alive) {
      return;
    }

    if (speedTimer > 0) {
      speedTimer --;
    } else if (speedTimer == 0 && speed != DEFAULT_SPEED) {
      speed = DEFAULT_SPEED;
    }

    Location last = playerLocations.get(playerLocations.size()-1);
    Location next = null;

    // For the speed, iterate x blocks between last and next

    if (direction == UPKEY)
    {
      next = getLocation(last.getX(), (int)(last.getY() - speed * getPixelSize()));
    } else if (direction == DOWNKEY)
    {
      next = getLocation(last.getX(), (int)(last.getY() + speed * getPixelSize()));
    } else if (direction == LEFTKEY)
    {
      next = getLocation((int)(last.getX() - speed * getPixelSize()), last.getY());
    } else if (direction == RIGHTKEY)
    {
      next = getLocation((int)(last.getX() + speed * getPixelSize()), last.getY());
    }

    ArrayList<Location> line = getLine(last, next);

    if (line.size() == 0) {
      gameOver();
      return;
    }

    for (Location loc : line) {
      if (checkCrash (loc)) {
        gameOver();
        return;
      } else {
        // Former bug: For some reason when a player eats a powerup a hole appears in the line where the powerup was.
        Location l2 = getLocation(loc.getX(), loc.getY());
        l2.setType(LocationType.PLAYER);
        l2.setColor(this.col);

        playerLocations.add(l2);
        getGridCache().add(l2);
      }
    }
  }

  // Stops the player from moving and subtracts 1 from their life pool
  public void gameOver() {
    this.lives --;
    this.alive = false;
  }

  // (Re)spawns ths player in the arena and resets the relevant variables. Note that there is a very small chance of two players spawning on one another -- find a workaround for this later.
  public void respawn(int x, int y) {
    if (x % getPixelSize() != 0 || y % getPixelSize() != 0) {
      throw new IllegalArgumentException();
    }

    this.direction = RIGHTKEY;
    this.alive = true;
    this.speed = DEFAULT_SPEED;
    this.playerLocations = new ArrayList();
    this.playerLocations.add(new Location(x, y, this.col, LocationType.PLAYER));
  }

  public void respawn(Location loc) {
    respawn(loc.getX(), loc.getY());
  }


  // Checks if the input char is one of the player's direction keys
  public boolean isKey(int dir) {
    return (dir == UPKEY || dir == DOWNKEY || dir == RIGHTKEY || dir == LEFTKEY);
  }

  // Switches the player's direction
  public void changeDirection (int dir) {
    if ((dir == UPKEY || dir == DOWNKEY || dir == RIGHTKEY || dir == LEFTKEY) && validMove(dir))
    {
      direction = dir;
    }
  }


  // Checks if player can move in that direction. Prevents player from moving in the direction opposite of their current direction
  private boolean validMove(int dir) {
    return !(dir == UPKEY && direction == DOWNKEY || dir == DOWNKEY && direction == UPKEY || dir == LEFTKEY && direction == RIGHTKEY || dir == RIGHTKEY && direction == LEFTKEY);
  }


  // Checks if the player's next location will cause the player to crash [true if it will / false if it wont]
  private boolean checkCrash (Location next)
  {
    if (next == null) {
      return true;
    }

    Location last = playerLocations.get(playerLocations.size()-1);

    LocationType type = next.getType();
    //println(getLocation(next).getType()+" : "+next);
    if (type == LocationType.POWERUP) {
      PowerUp p = getPowerUp(next);

      if (p != null) { // Basically a workaround for the NPE
        // if (ENABLE_SOUND) {
        //   sfx.gainedPowerUp();
        // }
        addSpeed(1);
        speedTimer += (int) frameRate * 2;
        removePowerUp(p);
      }

      return false;
    }

    if ((type == LocationType.PLAYER || type == LocationType.WALL) ||
      (next.getY() != last.getY() && (direction == LEFTKEY || direction == RIGHTKEY)) ||
      (next.getX() != last.getX() && (direction == UPKEY || direction == DOWNKEY))) { // This is to prevent bike from wrapping around edge of grid, because grid is a 1d array
      //sfx.lostALife(); //Commented out because you hear a shrill sound at the end for some reason
      return true;
    }

    return false;
  }

  // CompareTo method for generating final leaderboard
  public int compareTo(Object player) {
    return ((Player) this).lives - ((Player) player).lives;
  }


  // Getter / Setter methods -- should make these stylistcally the same (either get/set, or just the variable [eg. setSpeed() vs speed()]
  public void addSpeed(int speed) {
    this.speed += speed;
  }

  public void setSpeed(int speed) {
    this.speed = speed;
  }

  public void setColor(int other) {
    this.col = other;
  }

  public int getColor() {
    return this.col;
  }

  public int lives() {
    return this.lives;
  }

  public void setPlayerName(String name) {
    this.name = name;
  }

  public String name() {
    return this.name;
  }

  public void setHasName() {
    this.hasName = true;
  }

  public boolean hasName() { // If player's name is complete
    return this.hasName;
  }

  public boolean isAlive() {
    return this.alive;
  }

  public ArrayList<Location> locations() {
    return this.playerLocations;
  }
}


class PowerUp
{
  Random generator = new Random ();
  ArrayList <Location> imageLocs;
  PImage img;
  int xC;
  int yC;
  int xLength;
  int yLength;

  PowerUp(int a, int b, int c, int d) {
    img = loadImage ("PowerUp.png");
    xC = a;
    yC = b;
    xLength = c;
    yLength = d;
    createList();
  }
  
  //returns the list to locations of the power up image
  public ArrayList<Location> getLocations() {
    return this.imageLocs; 
  }
  
  //draws or renders the powerup on the grid
  public void render () {
    image (img, xC, yC, xLength * getPixelSize(), yLength * getPixelSize());
  }
  
  //adds each location in imageLocs to the gridCache
  public void addToCache () {
    ArrayList <Location> gridCache = getGridCache();
    for (Location l : imageLocs) {
      gridCache.add (l);
    }
  }
  
  //creates and returns a list of PowerUps
  public ArrayList <PowerUp> getPowerUps (int low, int high, int h, int w) {
    createPowerUps (low, high, h , w);
    return powerUps;
  }

  //creates a list of PowerUps
  public void createList () {
    imageLocs = new ArrayList <Location> ();
    for (int i = xC; i < xLength * getPixelSize() + xC; i+=getPixelSize()) {
      for (int j = yC; j < yLength * getPixelSize() + yC; j+=getPixelSize()) {
        Location loc = new Location (i, j, LocationType.POWERUP, true);

        imageLocs.add (loc);
        getGridCache().add(loc);
      
      }
    }
  }
}
class ScoreBar { //<>// //<>//

  ArrayList<Player> players;
  ArrayList<String> messages;
  PFont f;
  int x;
  int y;

  // Top bar of the game; can move this somewhere else later
  ScoreBar(ArrayList<Player> players, int x, int y) {
    // FORMAT: <name> [***] : 
    this.players = players;
    this.x = x;
    this.y = y;
    this.f = createFont("HelveticaNeue-Thin", 1, true);
  }

  // Draws the scorebar which appears at the top of the screen when the game isrun
  public void render() {
    stroke(color(50, 50, 50));
    fill(color(50, 50, 50));
    rect(0, 0, getWidth(), getTopHeight());
    int tempX = this.x;
    int fontSize = 150 / (this.players.size() * 2);

    this.messages = new ArrayList();
    int count = 0;
    for (Player player : this.players) {

      StringBuilder bar = new StringBuilder();
      // max lives = 3
      bar.append(player.name() + " ");
      for (int i=0; i<3; i++) {
        if (i < player.lives()) {
          bar.append('\u25AE');  // http://jrgraphix.net/r/Unicode/?range=25A0-25FF
        } else {
          bar.append('\u25AF');
        }
      }

      String message = bar.toString();
      messages.add(message);

      textAlign(LEFT);
      textFont(f, fontSize);
      fill(player.getColor());
      text(message, tempX, this.y);

      int newX = (int) textWidth(message) + 10;
      tempX += newX;
      count++;
    }
  }
}
// //Sounds taken from http://www.moviesoundclips.net/sound.php?id=235, https://www.youtube.com/playlist?list=PL7F37BB1A67E0238D and http://soundbible.com/1636-Power-Up-Ray.html
//
// // import processing.sound.*;
//
// //String dataPath = sketchPath() + "/data/";
//
//
// //For in game sound effects
// class SoundFX {
//
//   //Plays start of game sound
//   void preGame () {
//     preGame.loop();
//   }
//
//   String chooseGameSound () {
//     if (Math.random() > 0.2) {
//       return "TheGrid.mp3";
//     }
//
//     return "InGame.mp3";
//   }
//
//   //Transitions sounds from pre game in to in game, implementation of shiftGain (gain from, gain to, time in milliseconds);
//   void moveToGame () {
//     preGame.stop();
//     readyToPlay.play();
//     gameSound.loop();
//   }
//
//   //Plays sound FX for power ups
//   void gainedPowerUp () {
//     powerUp.play();
//   }
//
//   //Plays sound for losing a life
//   void lostALife () {
//     gameOver.play();
//   }
//
//   ////Sound FX for end of game
//   void endGame () {
//     gameSound.stop();
//     preGame.stop();
//     postGame.loop();
//   }
// }
class TextBox{
  String input;
  Player player;
  int col = color(50,50,200);
  PFont f;
  char BLANK_KEY = ')';
  
  public TextBox(Player player){
    this.input = player.name();
    
    this.player = player;
    f = createFont("HelveticaNeue-Light", 60, true);
  }

  public void draw() {
      background(color(0,0,0));
      textFont(f);
      
      fill(col);
      textAlign(CENTER);
      text(input, width/2, height/2);
      /*String underscore = "_";
      int charLength = 0;
      if (input.length() > 0) {
        charLength = (int) textWidth(input.substring(input.length()-1, input.length()));
      }
      text(underscore, width/2 + textWidth(input)/2 + charLength, height/2 + 30);*/
      fill(0xffE3E3E3);
      textSize(35);
      text("Please enter a name between 1-10 characters", width/2, 50);
      text("Your Controls: "+player.getControlKeys(), width/2, 90);
  }

  public void keyPressed() {
    if (key == ENTER) {
      if (input.length() == 0) {
        println("Please enter at least one letter.");
      } else {
        println("Your name is: "+input);
        col = color(0,255,0);
        this.player.setHasName();
        //Screen.setStage(3);
      }
    } else if (key == BLANK_KEY) {
      return;
    } else if ((key == CODED || (int) key < 48) && key != BACKSPACE) { // valid char check
      return;
      //println("Please enter a valid character. key="+key+" code="+(int) key);
    } else if (input.length() > 0 && key == BACKSPACE) { // backspace delete
      
      player.setPlayerName(input.substring(0, input.length()-1));
    } else if (input.length() <= 10) { // add key
      input += key;
      player.setPlayerName(player.name() + key);
      this.draw();
    }
  }
}
class Wall {
  int x;
  int y;
  int h;
  int w;
  
  Wall(int x, int y, int h, int w) {
    this.x = x;
    this.y = y;
    this.h = h;
    this.w = w;
  }
  
  // Draws the wall on the game screen (at x,y with width w and height h)
  public void render() {
    int pixel = getPixelSize();
    for (int xx = x; xx<x+w; xx += pixel) {
      for (int yy = y; yy<y+h; yy += pixel) {
        Location next = getLocation(xx, yy);
        if (next != null) { // This way if a wall is spawned at the edge it's ok
          next.setColor(color(255,255,255));
          next.setType(LocationType.WALL);
          getGridCache().add(next);
        }
      }
    }
  }
}
  public void settings() {  size(800, 720); }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "Tron" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
