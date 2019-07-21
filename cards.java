PImage cimg;
public final int SHEET_LENGTH = 9;   // Number of cards in a row (or column) on the sheet
public final int NUM_CARDS = 81;     // Number of cards in the sheet
public final int CARD_WIDTH = 138;    // Width in pixels of a card
public final int CARD_HEIGHT = 97;   // Height in pixels of a card
public final int CARD_X_SPACER = 1;       // Space between cards in the x-direction on the sheet
public final int CARD_Y_SPACER = 1;       // Space between cards in the y-direction on the sheet
// Offsets into the sheet of cards
final int LEFT_OFFSET = 6;
final int TOP_OFFSET = 9;

// Properties of cards expressed as symbols
public enum CardColor { GREEN, PURPLE, RED };
public enum CardCount { ONE, TWO, THREE };
public enum CardFill { SOLID, STRIPED, OPEN };
public enum CardShape { CAPSULE, SQUIGGLY, DIAMOND };

// For locating cards on the play grid
public final int GRID_LEFT_OFFSET = 16;   // Distance from left to start drawing grid
public final int GRID_TOP_OFFSET = 72;    // Distance from top to start drawing grid
public final int GRID_X_SPACER = 8;       // Separation between cards horizontally
public final int GRID_Y_SPACER = 8;       // Separation between cards vertically
public final int BEGIN_COLS = 4;          // Beginning number of columns in the grid
public final int ROWS = 3;                // Number of rows in the grid
public final int MAX_COLS = 7;            // Maximum number of columns in the grid
public int currentCols = BEGIN_COLS;      // Important to program in general, but also good
                                          // for testing special cases (cardsInPlay == 3, e.g.)

// From left of window to right edge of grid
//public int grid_right = GRID_LEFT_OFFSET + currentCols * (CARD_WIDTH + GRID_X_SPACER);
// From top of window to bottom of grid
public final int GRID_BOTTOM = GRID_TOP_OFFSET + ROWS * (CARD_HEIGHT + GRID_Y_SPACER);

// From top of window to top of buttons
final int BUTTON_LEFT_OFFSET = GRID_LEFT_OFFSET;
final int BUTTON_TOP_OFFSET = GRID_BOTTOM + 16;
final int BUTTON_WIDTH = 200;
final int BUTTON_HEIGHT = 56;

// Four buttons: Add Cards, Find Set, New Game, Pause
public final int NUM_BUTTONS = 4;

Grid grid;
Deck deck;

// Score information
public PFont scoreFont;
public final color SCORE_FILL = #000000;    // Black RGB values; feel free to change
public int score;
public int SCORE_LEFT_OFFSET = GRID_LEFT_OFFSET;
public int SCORE_TOP_OFFSET = 25;

// Timer information
public PFont timerFont;
public final color TIMER_FILL = #000000;
public int runningTimer;
public int runningTimerEnd;
public final int TIMER_LEFT_OFFSET = SCORE_LEFT_OFFSET+256;
public final int TIMER_TOP_OFFSET = SCORE_TOP_OFFSET;

// Message information
public PFont messageFont;
public final color MESSAGE_FILL = #000000;    // Black RGB values; feel free to change
public int message;
public final int MESSAGE_LEFT_OFFSET = TIMER_LEFT_OFFSET+256;
public final int MESSAGE_TOP_OFFSET = TIMER_TOP_OFFSET;

// Directions information
public PFont keyOptionsFont;
public final color KEY_OPTIONS_FILL = #000000;
public final int KEY_OPTIONS_LEFT_OFFSET = GRID_LEFT_OFFSET;
public final int KEY_OPTIONS_TOP_OFFSET = BUTTON_TOP_OFFSET + BUTTON_HEIGHT + 48;
public final String keyOptions = "q, w, e, r, [t, y, u]: top row;\na, s, d, f, [g, h, j]: second row;\n" +
                                 "z, x, c, v, [b, n, m]: third row\n" +
                                 "+ to add cards, - to find a set, SPACE to pause, ENTER/RETURN for a new game";

public final color BACKGROUND_COLOR = color(189, 195, 199);
public final color SELECTED_HIGHLIGHT = #FFDD00;
public final color CORRECT_HIGHLIGHT = #00FF00;
public final color INCORRECT_HIGHLIGHT = #FF0000;
public final color FOUND_HIGHLIGHT = #11CCCC;
public final int HIGHLIGHT_TICKS = 35;
public final int FIND_SET_TICKS = 60;
public int highlightCounter = 0;

// TIMER
public int gameTimer = 0;
public int setTimer = 0;
public int runningTimerStart;
public int timeElapsed = 0;


// state:
//   0 -> Normal play
//   1 -> Three cards selected (for freezing highlights)
//   2 -> Find Set selected
//   3 -> Game Over
//   4 -> Game Paused
public enum State { PLAYING, EVAL_SET, FIND_SET, GAME_OVER, PAUSED };
State state = State.PLAYING;

void setup() {
  size(1056, 568, P3D);
  background(BACKGROUND_COLOR);
  
  fill(#000000);
  text("Loading...", 50, 150);
  
  newGame();

  initFonts();  
  
  initSpriteSheet();
}

void draw() {
  background(BACKGROUND_COLOR);
  
  showScore();
  showTimer();
  showMessage();
  drawButtons();
  drawDirections();
  
  grid.display();
  grid.highlightSelectedCards();
  
  if (grid.tripleSelected() && state == State.PLAYING) {
    state = State.EVAL_SET;
    highlightCounter = 0;
  }
  
  // Three cards selected; process them
  if (state == State.EVAL_SET) {
    if (highlightCounter == HIGHLIGHT_TICKS) {  // 35 ticks showing special highlight
      grid.processTriple();
    } else {
      highlightCounter = highlightCounter+1;
    }
  // Find Set selected
  } else if (state == State.FIND_SET) {
    if (highlightCounter == FIND_SET_TICKS) {  // 35 ticks showing special highlight
      state = State.PLAYING;
      grid.clearSelected();
      score -= 5;
    } else {
      highlightCounter = highlightCounter + 1;
    }
  }
}

// For details on the 8-argument version of image(), see:
// https://forum.processing.org/one/topic/image-ing-a-part-of-a-pimage.html
void drawCard(int cardCol, int cardRow, int xpos, int ypos) {
  image(cimg, xpos, ypos, CARD_WIDTH+CARD_X_SPACER, CARD_HEIGHT+CARD_Y_SPACER,
              LEFT_OFFSET + cardCol*CARD_WIDTH, TOP_OFFSET + cardRow*CARD_HEIGHT, 
              (cardCol+1)*CARD_WIDTH+CARD_X_SPACER, (cardRow+1)*CARD_HEIGHT+CARD_Y_SPACER);
}

void drawRow(int row) {
  for (int col = 0; col < SHEET_LENGTH; col++)  {
    drawCard(col, row, col*(CARD_WIDTH+CARD_X_SPACER), row*(CARD_HEIGHT+CARD_Y_SPACER));
  }
}

void drawDeck() {
  for (int row = 0; row < ROWS; row++) {
    drawRow(row);
  }
}

void drawCards() {
  for (int row = 0; row < SHEET_LENGTH; row++) {
    drawRow(row);
  }
}
      
void drawButtons() {
  // Start, Stop, Clear rectangles in gray
  fill(#DDDDDD);
  for (int i = 0; i < NUM_BUTTONS; i++) {
    rect(BUTTON_LEFT_OFFSET+i*(BUTTON_WIDTH+12), BUTTON_TOP_OFFSET, BUTTON_WIDTH, BUTTON_HEIGHT);
  }
  
  // Set text color on the buttons to blue
  fill(#0000FF);

  text("Add Cards", BUTTON_LEFT_OFFSET+18, BUTTON_TOP_OFFSET+22); 
  text(" Find Set", BUTTON_LEFT_OFFSET+18+BUTTON_WIDTH+12, BUTTON_TOP_OFFSET+22); 
  text("New Game", BUTTON_LEFT_OFFSET+18+2*(BUTTON_WIDTH+12), BUTTON_TOP_OFFSET+22); 
  if (state == State.PAUSED) {
    text("Resume", BUTTON_LEFT_OFFSET+45+3*(BUTTON_WIDTH+12), BUTTON_TOP_OFFSET+22);
  } else {
    text("Pause", BUTTON_LEFT_OFFSET+54+3*(BUTTON_WIDTH+12), BUTTON_TOP_OFFSET+22);
  }
}

public void newGame() {
 deck = new Deck();
 grid = new Grid();
 score = 0;
 currentCols = 4;
 state = State.PLAYING;
 message = 0;
 for(int col = 0; col<currentCols; col++){
   for(int row = 0;row<ROWS;row++){
     Card card = new Card(col, row);
     grid.addCardToBoard(card);
   }
 }
 timeElapsed = 0;
 runningTimerStart = millis();
}

public void initFonts() {
  scoreFont = createFont("ComicSansMS-Bold", 32);
  messageFont = scoreFont;
  timerFont = scoreFont;
  keyOptionsFont = createFont("Times New Roman", 14);
  textAlign(LEFT, CENTER);
}

public void drawDirections() {
  fill(KEY_OPTIONS_FILL);
  textFont(keyOptionsFont);
  text(keyOptions, KEY_OPTIONS_LEFT_OFFSET, KEY_OPTIONS_TOP_OFFSET);
}

public void initSpriteSheet() {
  // NOTE: These cards are being used for educational purposes only and are not to be used
  // for profit without written consent by copyright holder(s).
  // Attribution for SET card sprite sheet:
  // url = "https://amiealbrecht.files.wordpress.com/2016/08/set-cards.jpg?w=1250";
  //
  // Set up cards on local machine to minimize loading time
  String cardSpriteSheet = "set-cards.jpg";
  // Need string that says "Loading..." here
  cimg = loadImage(cardSpriteSheet, "png");  
  // System.out.println(cimg);  //verify not null so we can be sure we have card sprites
}

void showScore() {
  textFont(scoreFont);
  fill(SCORE_FILL);
  text("Score: " + score, SCORE_LEFT_OFFSET, SCORE_TOP_OFFSET);
}

public void showMessage() {
  textFont(messageFont);
  String str = "";
  switch(message) {
    case 0: str = "Welcome to SET!"; break;
    case 1: str = "Set found!"; break;
    case 2: str = "Sorry, not a set!"; break;
    case 3: str = "Cards added to board..."; break;
    case 4: str = "There is a set on the board!"; break;
    case 5: str = "No cards left in the deck!"; break;
    case 6: str = "No set on board to find!"; break;
    case 7: str = "GAME OVER!"; break;
    case 8: str = "\"" + key + "\"" + " not an active key!"; break;
    case 9: str = "Game paused"; break;
    case 10: str = "Game resumed"; break;
    default: str = "Something is wrong. :-(";
  }
  text(str, MESSAGE_LEFT_OFFSET, MESSAGE_TOP_OFFSET);
}

public class Grid {
  // In the physical SET game, cards are placed on the table.
  // The table contains the grid of cards and is typically called the board.
  Card[][] board = new Card[MAX_COLS][ROWS];   // Array that contains cards
  
  ArrayList<Location> selectedLocs = new ArrayList<Location>();  // Locations selected by the player
  ArrayList<Card> selectedCards = new ArrayList<Card>();         // Cards selected by the player 
                                                                 // (corresponds to the locations)  
  int cardsInPlay;    // Number of cards visible on the board

  public Grid() { 
    cardsInPlay = 0;
  }


  // GRID MUTATION PROCEDURES
  
  // 1. Highlight (or remove highlight) selected card
  // 2. Add (or remove) the location of the card in selectedLocs
  // 3. Add the card to (or remove from) the list of selectedCards
  public void updateSelected(int col, int row) {
    Card card = board[col][row];

    if (selectedCards.contains(card)) {
      int index = selectedCards.indexOf(card);
      selectedLocs.remove(index);
      selectedCards.remove(card);
      //score--;
    } else {
      selectedLocs.add(new Location(col, row));
      selectedCards.add(card);
    }

    //System.out.println("Cards = " + selectedCards + ", Locations = " + selectedLocs);
  }

  // Precondition: A Set has been successfully found
  // Postconditions: 
  //    * The number of columns is adjusted as needed to reflect removal of the set
  //    * The number of cards in play is adjusted as needed
  //    * The board is mutated to reflect removal of the set
  public void removeSet() {
    // Because it seems to make for a better UX, cards should not change locations unless
    // the number of columns has decreased.  If that happens, cards from the rightmost
    // column should be moved to locations where cards that formed the selected set
    // Put the locations of the selected cells in order.  Cards from the rightmost column
    // that are part of the set should be removed instead of being migrated.
    
    selectedLocs.sort(null);  // Don't delete this line as it orders the selected locations
                              // You may wish to look up how the Location class decides
                              // how to compare two different locations.  Also look up the
                              // documentation on ArrayList to see how sort(null) works
     if(deck.size() == 0 || cardsInPlay > 12){
           int lastc = (cardsInPlay/(ROWS+1))-1;
           int lastr = (cardsInPlay/(currentCols+1))-1;
           for(int i = lastr; i>-1;i--){
             board[selectedLocs.get(i).getCol()][selectedLocs.get(i).getRow()] = board[i][lastc];
             //board[i][lastc] = selectedCards.get(i);
           }
           currentCols--;
     }
     else if(cardsInPlay == 12){
       //System.out.println("column = " + selectedLocs.remove(0).getCol());
       //System.out.println("row = " +selectedLocs.remove(0).getRow());
       //System.out.println(board[1][0]);
       
       for(int i = 0;i<3;i++){
       board[selectedLocs.get(i).getCol()][selectedLocs.get(i).getRow()] = deck.deal();
       }
     }
     selectedLocs.clear();
     selectedCards.clear();

  }
  
  // Precondition: Three cards have been selected by the player
  // Postcondition: Game state, score, game message mutated, selected cards list cleared
  public void processTriple() {
    if (isSet(selectedCards.get(0), selectedCards.get(1), selectedCards.get(2))) {
      score += 10;
      removeSet();
      if (isGameOver()) {
        state = State.GAME_OVER;
        showTimer();
        score += timerScore();
        message = 7;
      } else {
        state = State.PLAYING;
        message = 1;
      }
    } else {
      score -= 5;
      state = State.PLAYING;
      message = 2;
    }
    clearSelected();
  }
  
  
  // DISPLAY CODE
  
  public void display() {
    int cols = cardsInPlay / 3;
    for (int col = 0; col < cols; col++) {
      for (int row = 0; row < ROWS; row++) {
        board[col][row].display(col, row);
      }
    }
  }

  public void highlightSelectedCards() {
    color highlight;
    if (state == State.FIND_SET) {
      highlight = FOUND_HIGHLIGHT;
      selectedLocs = findSet();
      if (selectedLocs.size() == 0) {
        message = 6;
        return;
      }
    } else if (selectedLocs.size() < 3) {
      highlight = SELECTED_HIGHLIGHT;
    } else {
      highlight = isSet(selectedCards.get(0), selectedCards.get(1), selectedCards.get(2)) ?
                  CORRECT_HIGHLIGHT :
                  INCORRECT_HIGHLIGHT;
    }
    for (Location loc : selectedLocs) {
      drawHighlight(loc, highlight);
    }
  }
  
  public void drawHighlight(Location loc, color highlightColor) {
    stroke(highlightColor);
    strokeWeight(5);
    noFill();
    int col = loc.getCol();
    int row = loc.getRow();
    rect(GRID_LEFT_OFFSET+col*(CARD_WIDTH+GRID_X_SPACER), 
      GRID_TOP_OFFSET+row*(CARD_HEIGHT+GRID_Y_SPACER), 
      CARD_WIDTH, 
      CARD_HEIGHT);
    stroke(#000000);
    strokeWeight(1);
  }

  
  // DEALING CARDS

  // Preconditions: cardsInPlay contains the current number of cards on the board
  //                the array board contains the cards that are on the board
  // Postconditions: board has been updated to include the card
  //                the number of cardsInPlay has been increased by one
  public void addCardToBoard(Card card) {
    int c = (int) cardsInPlay/(ROWS+1);
    int r = cardsInPlay % (ROWS+1);
    board[r][c] = card;
    cardsInPlay++;
  }
    
  public void addColumn() {
    if(cardsInPlay >= deck.size()- ROWS){
      message = 5;
      return;
    }
    else if(findSet()==null){
      score += 5;
      for(int i = 0; i<3; i++){
        addCardToBoard(deck.deal());
        }
      currentCols += 3;
      message = 3;
    }
    score -= 5;
    message = 4;
  }

  
  // GAME PROCEDURES
  
  public boolean isGameOver() {
    if(deck.size() == 0 && findSet().isEmpty()){
      return true;
    }
    return false;
  }

  public boolean tripleSelected() {
    return (selectedLocs.size() == 3);
  }
   
  // Preconditions: --
  // Postconditions: The selected locations and cards ArrayLists are empty
  public void clearSelected() {
    selectedLocs.clear();
    selectedCards.clear();
  }
  
  // findSet(): If there is a set on the board, existsSet() returns an ArrayList containing
  // the locations of three cards that form a set, an empty ArrayList (not null) otherwise
  // Preconditions: --
  // Postconditions: No change to any state variables
  public ArrayList<Location> findSet() {
    ArrayList<Location> locs = new ArrayList<Location>();
    for (int i = 0; i < currentCols*3 - 2; i++) {
      for (int j = i+1; j < currentCols*3 - 1; j++) {
        for (int k = j+1; k < currentCols*3; k++) {
          if (isSet(board[col(i)][row(i)], board[col(j)][row(j)], board[col(k)][row(k)])) {
            locs.add(new Location(col(i), row(i)));
            locs.add(new Location(col(j), row(j)));
            locs.add(new Location(col(k), row(k)));
            return locs;
          }
        }
      }
    }
    return new ArrayList<Location>();
  }

  
  // UTILITY FUNCTIONS FOR GRID CLASS
  
  public int col(int n) {
    return n/3;
  }
  
  public int row(int n) {
    return n % 3;
  }
   
  public int rightOffset() {
    return GRID_LEFT_OFFSET + currentCols * (CARD_WIDTH + GRID_X_SPACER);
  }
}

boolean sameColor(Card a, Card b, Card c) {
  if(a.getColor().equals(b.getColor()) && b.getColor().equals(c.getColor())){
    return true;
  }
  return false;
}

boolean sameShape(Card a, Card b, Card c) {
    if(a.getShape().equals(b.getShape()) && b.getShape().equals(c.getShape())){
    return true;
  }
  return false;
}

boolean sameFill(Card a, Card b, Card c) {
    if(a.getFill().equals(b.getFill()) && b.getFill().equals(c.getFill())){
    return true;
  }
  return false;
}

boolean sameCount(Card a, Card b, Card c) {
    if(a.getCount().equals(b.getCount()) && b.getCount().equals(c.getCount())){
    return true;
  }
  return false;
}

boolean diffColor(Card a, Card b, Card c) {
    if(!(a.getColor().equals(b.getColor())) && !(b.getColor().equals(c.getColor())) && !(a.getColor().equals(c.getColor()))){
    return true;
  }
  return false;
}

boolean diffShape(Card a, Card b, Card c) {
  if(!(a.getShape().equals(b.getShape())) && !(b.getShape().equals(c.getShape())) && !(a.getShape().equals(c.getShape()))){
    return true;
  }
  return false;
}

boolean diffFill(Card a, Card b, Card c) {
  if(!(a.getFill().equals(b.getFill())) && !(b.getFill().equals(c.getFill())) && !(a.getFill().equals(c.getFill()))){
    return true;
  }
  return false;
}

boolean diffCount(Card a, Card b, Card c) {
  if(!(a.getCount().equals(b.getCount())) && !(b.getCount().equals(c.getCount())) && !(a.getCount().equals(c.getCount()))){
    return true;
  }
  return false;
}  

boolean isSet(Card a, Card b, Card c) {
  if((sameColor(a,b,c)||diffColor(a,b,c))&&(sameShape(a,b,c)||diffShape(a,b,c))&&(sameFill(a,b,c)||diffFill(a,b,c))&&(sameCount(a,b,c)||diffCount(a,b,c))){
    return true;
}
return false;
}

public void togglePauseResume() {
  if (state == State.PAUSED) {
    resumeGame();
  } else {
    pauseGame();
  }
}

public void pauseGame() {
  state = State.PAUSED;
  timeElapsed += millis() - runningTimerStart;
  message = 9;
}

public void resumeGame() {
  state = State.PLAYING;
  runningTimerStart = millis();
  message = 10;
}

void showTimer() {
  textFont(timerFont);
  fill(TIMER_FILL);
  // If the game is paused, show time elapsed
  // If the game is over, show time to complete
  // Otherwise, show time elapsed so far in current game
  if (state == State.PAUSED) {
    text("Time: " + timeElapsed/1000, TIMER_LEFT_OFFSET, TIMER_TOP_OFFSET);
  } else if (state == State.GAME_OVER) { 
    text("Time: " + (runningTimerEnd - runningTimerStart + timeElapsed)/1000, TIMER_LEFT_OFFSET, TIMER_TOP_OFFSET);
  } else {
    text("Time: " + (millis() - runningTimerStart + timeElapsed)/1000, TIMER_LEFT_OFFSET, TIMER_TOP_OFFSET);
  }
}

public int timerScore() {
  // Returns the number of points scored based on the timer, which is
  // the GREATER of:
  //    300 minus the number of seconds taken when the game ends
  //      0
  //
  // If it took 277 seconds to finish the game, this should return 23 (300-277=23)
  // If it took 435 seconds to finish the game, this should return 0 (435 > 300)
   int sec = (int)(timeElapsed/1000);
  if(300 - sec > 0){
    return sec;
  }
  return 0;
    
}


