/*Team 9 - W. Robleh, M. Mariscal, T. Doan, Y. Nikulyak, C. Piwarski
 * CST338 - Software Design
 * Assignment 6
 * This assignments builds upon on GUI game implementation by converting
 * everything to a Model-View-Controller Standard, adding multi-threading, and
 * implementing a timer for the game.
 */

import java.awt.*;
import java.awt.event.*;
import javax.swing.border.*;
import javax.swing.*;

public class BUILD
{
   public static void main(String[] args)
   {
      @SuppressWarnings("unused")
      Controller gameController = new Controller();
   }
}

/*
 * One object of class model contains the logic for the card game.
 */
class Model
{
   Controller gameController;

   /*
    * Constructor method for the Model class. This method receives a Controller
    * object and saves the reference.
    */
   public Model(Controller gameController)
   {
      this.gameController = gameController;
   }

   /*
    * This method generates a random Card object.
    */
   public static Card generateRandomCard()
   {
      Deck deck = new Deck();
      deck.shuffle();
      return deck.dealCard();
   }

   /*
    * Makes human move and tests the game logic. Method processes two states: when
    * a human is to make the first move (lastPlayedCard is null) and when a
    * computer has previously made its move (lastPlayedCard} is not null).
    */
   public void makeHumanMove(int cardIndex)
   {
      if (cardIndex >= gameController.getGame().getHand(1).getNumCards())
      {
         return;
      }

      Card cardToPlay = gameController.getGame().getHand(1).playCard(cardIndex);
      System.out.println("Player is playing: " + cardToPlay);
      gameController.requestSetPlayedCard(1, cardToPlay);

      Card computerCard;
      if (gameController.getLastPlayedCard() != null)
      {
         computerCard = gameController.getLastPlayedCard();
         gameController.setLastPlayedCard(null);
      } else
      {
         computerCard = makeComputerMove(gameController.getGame());
      }

      // check mini winner for every 2 cards
      if (cardToPlay.compareTo(computerCard) < 0)
      {
         gameController.incrementPlayerScore(0);
         gameController.setComputerWinner(true);
         gameController.setPlayerWinner(false);
      } else
      {
         gameController.incrementPlayerScore(1);
         gameController.setComputerWinner(false);
         gameController.setPlayerWinner(true);
      }

      this.gameController.requestPlayerHandRedraw();
      this.gameController.requestScoreRedraw();

      Timer timer = new Timer(2000, gameController.getNewDelayedListener());
      timer.setRepeats(false);
      timer.start();
   }

   /*
    * Checks round and game completion after timeout in (DelayedGameCheckListener),
    * sets card places with the back of the cards, makes computer move if computer
    * won last round and remembers the result. The method is called every time 2
    * cards are played.
    */
   public void afterAllMoved()
   {
      checkRoundFinished();
      checkGameFinished();

      gameController.requestResetPlayedCards();

      if (Controller.compWinner)
      {
         Controller.lastPlayedCard = makeComputerMove(gameController.getGame());
      }
   }

   /*
    * Makes computer move, updates card place of computer and returns card played
    * by computer.
    */
   public Card makeComputerMove(CardGameFramework game)
   {
      int computerCardIndex = chooseCompHighestCard(game);

      Card computerCard = game.getHand(0).playCard(computerCardIndex);

      // delay quick change of computers cards so human
      // can have a time to see results
      Timer timer = new Timer(300, this.gameController.getRefreshCompCardListener(computerCard));
      timer.setRepeats(false);
      timer.start();
      System.out.println("Computer is playing: " + computerCard);
      return computerCard;
   }

   /*
    * return Highest card index in computer's hand or -1 if no cards left.
    */
   public static int chooseCompHighestCard(CardGameFramework game)
   {
      int highestCardIndex = -1;
      Card currentCard = null;
      for (int i = 0; i < game.getHand(0).getNumCards(); i++)
      {
         if (currentCard == null)
         {
            currentCard = game.getHand(0).inspectCard(i);
            highestCardIndex = i;
         }
         if (game.getHand(0).inspectCard(i).compareTo(currentCard) > 0)
         {
            currentCard = game.getHand(0).inspectCard(i);
            highestCardIndex = i;
         }
      }
      return highestCardIndex;
   }

   /*
    * if no more cards in hands then round finished and we deal, reset scores and
    * redraw human hand
    */
   public void checkRoundFinished()
   {
      if (gameController.getGame().getHand(0).getNumCards() <= 0
            && gameController.getGame().getHand(1).getNumCards() <= 0)
      {
         if (gameController.getPlayerScore(0) > gameController.getPlayerScore(1))
         {
            gameController.computerWinsRound();
            gameController.incrementTotalScore(0);
         } else
         {
            gameController.playerWinsRound();
            gameController.incrementTotalScore(1);
         }

         gameController.getGame().deal();
         resetScores();
         this.gameController.requestPlayerHandRedraw();
      }
   }

   /*
    * if no more cards in deck the game is over, check who wins
    */
   public void checkGameFinished()
   {
      if (gameController.getGame().getHand(1).getNumCards() <= 0
            && gameController.getGame().getNumCardsRemainingInDeck() <= 0)
      {
         if (gameController.getTotalScore(0) > gameController.getTotalScore(1))
         {
            gameController.computerWinsGame();
         } else if (gameController.getTotalScore(0) < gameController.getTotalScore(1))
         {
            gameController.playerWinsGame();
         } else
         {
            gameController.tieGame();
         }
         Controller.playerWinner = false;
         Controller.compWinner = false;
      }
   }

   /*
    * refresh score after each round to start count against to see who will win
    */
   private static void resetPlayerScores()
   {
      for (int i = 0; i < Controller.NUM_PLAYERS; i++)
      {
         Controller.setPlayerScore(i, 0);
      }
   }

   /*
    * This method resets both the player scores and the total scores.
    */
   public void resetScores()
   {
      resetPlayerScores();
      resetTotalScores();
   }

   /*
    * reset winner/loser total scores after game ends
    */
   private void resetTotalScores()
   {
      for (int i = 0; i < Controller.NUM_PLAYERS; i++)
      {
         Controller.setTotalScore(i, 0);
      }
   }
}

/*
 * One object of class View contains the graphical user interface for the card
 * game.
 */
@SuppressWarnings("serial")
class CardTableView extends JFrame
{

   static JLabel[] computerLabels = new JLabel[Controller.NUM_CARDS_PER_HAND];
   static JLabel[] humanLabels = new JLabel[Controller.NUM_CARDS_PER_HAND];
   static JLabel[] playedCardLabels = new JLabel[Controller.NUM_PLAYERS];
   static JLabel[] playLabelText = new JLabel[Controller.NUM_PLAYERS];
   static JLabel[] playerScoresLabels = new JLabel[Controller.NUM_PLAYERS];

   private int numCardsPerHand;
   private int numPlayers;

   public JPanel pn1ComputerHand, pn1HumanHand, pn1PlayerArea, buttonPanel, mainPanel;

   public JButton exitButton, newGameButton, cannotPlayButton;

   Controller gameController;

   /*
    * Constructor method for class CardTableView initializes the JFrame for the
    * Card game.
    */
   public CardTableView(Controller gameController, String title, int numCardsPerHand, int numPlayers)
   {

      super(title);

      // test parameters validity
      if (numCardsPerHand < 0 || numCardsPerHand > Controller.MAX_CARDS_PER_HAND || numPlayers < 0
            || numPlayers > Controller.MAX_PLAYERS)
      {
         return;
      }

      // define main frame attributes
      this.gameController = gameController;
      this.numCardsPerHand = numCardsPerHand;
      this.numPlayers = numPlayers;
      this.setSize(700, 650);
      this.setLocationRelativeTo(null);
      this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

      int k;

      // create button panel
      this.buttonPanel = new JPanel();
      this.buttonPanel.setLayout(new BorderLayout());
      this.exitButton = new JButton("Exit Game");
      this.exitButton.setPreferredSize(new Dimension(250, 25));
      this.newGameButton = new JButton("Start New Game");
      this.newGameButton.setPreferredSize(new Dimension(250, 25));
      this.cannotPlayButton = new JButton("I Cannot Play");
      this.cannotPlayButton.setPreferredSize(new Dimension(150, 25));
      this.buttonPanel.add(this.exitButton, BorderLayout.WEST);
      this.buttonPanel.add(this.cannotPlayButton, BorderLayout.CENTER);
      this.buttonPanel.add(this.newGameButton, BorderLayout.EAST);
      this.add(buttonPanel);

      // layout computer player hands
      pn1ComputerHand = new JPanel();
      pn1ComputerHand.setLayout(new GridLayout(1, numCardsPerHand));
      pn1ComputerHand.setBorder(new TitledBorder("Computer Hand"));

      // layout center playing area
      pn1PlayerArea = new JPanel();
      pn1PlayerArea.setLayout(new GridLayout(3, numPlayers));
      pn1PlayerArea.setBorder(new TitledBorder("Playing Area"));

      // layout human player hands
      pn1HumanHand = new JPanel();
      pn1HumanHand.setLayout(new GridLayout(1, numCardsPerHand));
      pn1HumanHand.setBorder(new TitledBorder("Your Hand"));

      this.mainPanel = new JPanel();
      this.mainPanel.setLayout(new BorderLayout());
      this.mainPanel.add(pn1ComputerHand, BorderLayout.NORTH);
      this.mainPanel.add(pn1PlayerArea, BorderLayout.CENTER);
      this.mainPanel.add(pn1HumanHand, BorderLayout.SOUTH);

      this.setLayout(new BorderLayout());
      this.add(this.buttonPanel, BorderLayout.NORTH);
      this.add(this.mainPanel, BorderLayout.CENTER);

      // CREATE LABELS ----------------------------------------------------
      // labels for computer
      for (k = 0; k < Controller.NUM_CARDS_PER_HAND; k++)
      {
         computerLabels[k] = new JLabel(GUICard.getBackCardIcon());
      }
      // labels for human
      for (k = 0; k < Controller.NUM_CARDS_PER_HAND; k++)
      {
         humanLabels[k] = new JLabel(GUICard.getIcon(gameController.getGame().getHand(1).inspectCard(k)));
      }

      // ADD LABELS TO PANELS -----------------------------------------
      // add computer labels
      for (k = 0; k < Controller.NUM_CARDS_PER_HAND; k++)
      {
         this.pn1ComputerHand.add(computerLabels[k]);
      }

      // add human labels
      for (k = 0; k < Controller.NUM_CARDS_PER_HAND; k++)
      {
         this.pn1HumanHand.add(humanLabels[k]);
      }

      // initial state for the game
      for (k = 0; k < Controller.NUM_PLAYERS; k++)
      {
         playedCardLabels[k] = new JLabel(GUICard.getBackCardIcon());
         playerScoresLabels[k] = new JLabel("0", SwingConstants.CENTER);
         playLabelText[k] = new JLabel(k == 0 ? "Computer" : "You", SwingConstants.CENTER);
      }

      for (k = 0; k < Controller.NUM_PLAYERS; k++)
      {
         this.pn1PlayerArea.add(playedCardLabels[k]);
      }

      for (k = 0; k < Controller.NUM_PLAYERS; k++)
      {
         this.pn1PlayerArea.add(playLabelText[k]);
      }
      for (k = 0; k < Controller.NUM_PLAYERS; k++)
      {
         this.pn1PlayerArea.add(playerScoresLabels[k]);
      }

      // show everything to the user
      this.setVisible(true);
   }

   /*
    * This method returns the Exit Button for the CardTable.
    */
   public JButton getExitButton()
   {
      return this.exitButton;
   }

   /*
    * This method receives an integer index and Card object. It sets the indexed
    * played card label with the provided Card's icon
    */
   public void setPlayedCard(int index, Card computerCard)
   {
      // CardTableView.playedCardLabels[0].setIcon(GUICard.getIcon(computerCard));
      CardTableView.playedCardLabels[index].setIcon(GUICard.getIcon(computerCard));
   }

   /*
    * This method receives an integer index and Card object. It sets the indexed
    * computer player label with the provided Card's icon
    */
   public void setComputerCard(int index, Card computerCard)
   {
      CardTableView.computerLabels[index].setIcon(GUICard.getIcon(computerCard));
   }

   /*
    * This method receives an integer index and Card object. It sets the indexed
    * human player label with the provided Card's icon
    */
   public void setPlayerCard(int index, Card playerCard)
   {
      CardTableView.humanLabels[index].setIcon(GUICard.getIcon(playerCard));
   }

   /*
    * This method returns the New Game Button for the CardTable.
    */
   public JButton getnewGameButton()
   {
      return this.newGameButton;
   }

   /*
    * This method returns the Cannot Play Button for the CardTable.
    */
   public JButton getCannotPlayButton()
   {
      return this.cannotPlayButton;
   }

   /*
    * This method returns the number of cards in a player's hand in integer form.
    */
   public int getnumCardPerHand()
   {
      return numCardsPerHand;
   }

   /*
    * This method resets played card labels to the back card icon.
    */
   public void resetPlayedCardLabels()
   {
      CardTableView.playedCardLabels[0].setIcon(GUICard.getBackCardIcon());
      CardTableView.playedCardLabels[1].setIcon(GUICard.getBackCardIcon());
   }

   /*
    * This method returns the number of players in integer form.
    */
   public int getnumPlayers()
   {
      return numPlayers;
   }

   /*
    * This method sets the player score label to declare that the human player won
    * the round.
    */
   public void setPlayerWinsText()
   {
      playerScoresLabels[1].setText(Controller.playerScores[1] + ": Player wins a round");
   }

   /*
    * This method sets the player score label to declare that the computer player
    * won the round.
    */
   public void setComputerWinsText()
   {
      playerScoresLabels[0].setText(Controller.playerScores[0] + ": Computer wins a round");
   }

   /*
    * show the new score after every 2 cards play
    */
   public void redrawScore()
   {
      for (int i = 0; i < Controller.NUM_PLAYERS; i++)
      {
         CardTableView.playerScoresLabels[i].setText(Integer.toString(Controller.playerScores[i]));
      }
   }

   /*
    * redraw human hand every time 2 cards played to show how many cards left in a
    * hand, or after a round ended to show again full hand of cards
    */
   public void redrawPlayerHand()
   {
      Hand hand = gameController.getGame().getHand(1);
      for (int k = 0; k < Controller.NUM_CARDS_PER_HAND; k++)
      {
         if (k >= hand.getNumCards())
         {
            CardTableView.humanLabels[k].setIcon(GUICard.getBackCardIcon());
         } else
         {
            CardTableView.humanLabels[k].setIcon(GUICard.getIcon(hand.inspectCard(k)));
         }
      }
   }

   /*
    * This method reconfigures the GUI to display that the computer player has won.
    */
   public void displayComputerWin()
   {
      CardTableView.playerScoresLabels[0].setFont(new Font("Serif", Font.BOLD, 18));
      CardTableView.playerScoresLabels[0].setText("Computer won a game");
      CardTableView.playerScoresLabels[1].setText("");
   }

   /*
    * This method reconfigures the GUI to display that the human player has won.
    */
   public void displayPlayerWin()
   {
      CardTableView.playerScoresLabels[1].setFont(new Font("Serif", Font.BOLD, 18));
      CardTableView.playerScoresLabels[1].setText("You won a game");
      CardTableView.playerScoresLabels[0].setText("");
   }

   /*
    * This method reconfigures the GUI to display a Tie Game has been reached.
    */
   public void displayTieGame()
   {
      CardTableView.playerScoresLabels[0].setFont(new Font("Serif", Font.BOLD, 18));
      CardTableView.playerScoresLabels[1].setFont(new Font("Serif", Font.BOLD, 18));
      CardTableView.playerScoresLabels[0].setText("Tie");
      CardTableView.playerScoresLabels[1].setText("Game");
   }
}

/*
 * One object of class Controller controls the GUI and Model for the card game.
 */
class Controller
{

   static final int MAX_CARDS_PER_HAND = 57;
   static int MAX_PLAYERS = 2;
   static int NUM_CARDS_PER_HAND = 7;
   static int NUM_PLAYERS = 2;
   static boolean playerWinner = false;
   static boolean compWinner = false;
   static Card lastPlayedCard = null;
   static int[] playerScores = new int[NUM_PLAYERS];
   static int[] totalScores = new int[NUM_PLAYERS];

   int numPacksPerDeck;
   int numJokersPerPack;
   int numUnusedCardsPerPack;
   Card[] unusedCardsPerPack;
   CardGameFramework highCardGame;

   public Model gameModel;
   public CardTableView gameView;

   /*
    * This is the constructor method for class Controller. It initializes the
    * button and mouse listeners needed to play the card game.
    */
   public Controller()
   {
      this.numPacksPerDeck = 1;
      this.numJokersPerPack = 0;
      this.numUnusedCardsPerPack = 0;
      this.unusedCardsPerPack = null;

      this.highCardGame = new CardGameFramework(numPacksPerDeck, numJokersPerPack, numUnusedCardsPerPack,
            unusedCardsPerPack, NUM_PLAYERS, NUM_CARDS_PER_HAND);
      this.highCardGame.deal();

      this.gameModel = new Model(this);
      this.gameView = new CardTableView(this, "CardTable", NUM_CARDS_PER_HAND, NUM_PLAYERS);

      // add mouse listener
      for (int k = 0; k < Controller.NUM_CARDS_PER_HAND; k++)
      {
         gameView.pn1HumanHand.add(CardTableView.humanLabels[k]);
         HandCardMouseListener listener = new HandCardMouseListener(k, this.highCardGame, this.gameModel);
         CardTableView.humanLabels[k].addMouseListener(listener);
      }

      // add button listener
      this.gameView.getExitButton().addActionListener(new GameButtonListener(this));
      this.gameView.getnewGameButton().addActionListener(new GameButtonListener(this));
      this.gameView.getCannotPlayButton().addActionListener(new GameButtonListener(this));

   }

   /*
    * This method receives an integer index and new score, and then sets that item
    * in player score to the new given score.
    */
   public static void setPlayerScore(int index, int newScore)
   {
      if (index >= 0 && index <= NUM_PLAYERS)
      {
         Controller.playerScores[index] = newScore;
      } else
         System.out.println("Error setting player score");
   }

   /*
    * This method receives an integer index and new score, and then sets that item
    * in total score to the new given score.
    */
   public static void setTotalScore(int index, int newScore)
   {
      if (index >= 0 && index <= NUM_PLAYERS)
      {
         Controller.playerScores[index] = newScore;
      } else
         System.out.println("Error setting player score");
   }

   /*
    * This method instructs the Controller object to redraw the player hand in the
    * CardTableView.
    *
    */
   public void requestPlayerHandRedraw()
   {
      gameView.redrawPlayerHand();
   }

   /*
    * This method receives an integer index and returns the integer associated
    * total score.
    */
   public int getTotalScore(int index)
   {
      if (index >= 0 && index <= NUM_PLAYERS)
      {
         return totalScores[index];
      } else
         return 0;
   }

   /*
    * This method receives an integer index and returns the integer associated
    * player score.
    */
   public int getPlayerScore(int index)
   {
      if (index >= 0 && index <= NUM_PLAYERS)
      {
         return playerScores[index];
      } else
         return 0;
   }

   /*
    * This method receives an integer index and increments that totalScore item in
    * the array.
    */
   public boolean incrementTotalScore(int index)
   {
      if (index >= 0 && index < NUM_PLAYERS)
      {
         Controller.totalScores[index]++;
         return true;
      } else
         return false;
   }

   /*
    * This method receives an integer index and increments that playerScore item in
    * the array.
    */
   public boolean incrementPlayerScore(int index)
   {
      if (index >= 0 && index < NUM_PLAYERS)
      {
         Controller.playerScores[index]++;
         return true;
      } else
         return false;
   }

   /*
    * This method instructs the Controller to have the CardTableView redraw the
    * scores.
    */
   public void requestScoreRedraw()
   {
      gameView.redrawScore();
   }

   /*
    * This method requests that the Controller resets the playedCardLabels to the
    * back card icons in the CardTableView.
    */
   public void requestResetPlayedCards()
   {
      gameView.resetPlayedCardLabels();
   }

   /*
    * This method instructs the game Model to reset the player and total scores.
    */
   public void requestResetScores()
   {
      gameModel.resetScores();
   }

   /*
    * This method requests that the Controller resets the playedCardLabels to the
    * back card icons in the CardTableView.
    */
   public void requestSetPlayedCard(int index, Card computerCard)
   {
      // CardTableView.playedCardLabels[0].setIcon(GUICard.getIcon(computerCard));
      gameView.setPlayedCard(index, computerCard);
   }

   /*
    * This method receives an array index and Card object. The controller then sets
    * the indexed player card icon in the view with the icon for the given Card.
    */
   public void requestSetPlayerCard(int index, Card playerCard)
   {
      gameView.setPlayerCard(index, playerCard);
   }

   /*
    * This method receives an array index and Card object. The controller then sets
    * the indexed Computer card icon in the view with the icon for the given Card.
    */
   public void requestSetComputerCard(int index, Card computerCard)
   {
      gameView.setComputerCard(index, computerCard);
   }

   /*
    * This method sets the player winner flag to the given boolean condition.
    */
   public boolean setPlayerWinner(boolean condition)
   {
      playerWinner = condition;
      return true;
   }

   /*
    * This method sets the computer winner flag to the given boolean condition.
    */
   public boolean setComputerWinner(boolean condition)
   {
      compWinner = condition;
      return true;
   }

   /*
    * This method returns the last played Card object.
    */
   public Card getLastPlayedCard()
   {
      return Controller.lastPlayedCard;
   }

   /*
    * This method receives a Card object and sets it as the last played Card.
    */
   public void setLastPlayedCard(Card newCard)
   {
      Controller.lastPlayedCard = newCard;
   }

   /*
    * This method returns the CardGameFramework stored in the this Controller.
    */
   public CardGameFramework getGame()
   {
      return this.highCardGame;
   }

   /*
    * This method handles all the controller activity for when the human player
    * wins a round.
    */
   public void playerWinsRound()
   {
      gameView.setPlayerWinsText();
      System.out.println("You win this round.");
   }

   /*
    * This method handles all the controller activity for when the computer player
    * wins a round.
    */
   public void computerWinsRound()
   {
      gameView.setComputerWinsText();
      System.out.println("Computer wins this round.");
   }

   /*
    * This method handles all the controller activity for when the computer player
    * wins a game.
    */
   public void computerWinsGame()
   {
      gameView.displayComputerWin();
   }

   /*
    * This method handles all the controller activity for when the human player
    * wins a game.
    */
   public void playerWinsGame()
   {
      gameView.displayPlayerWin();
   }

   /*
    * This method handles all the controller activity for when the game is a tie.
    */
   public void tieGame()
   {
      gameView.displayTieGame();
   }

   /*
    * This method returns a new DelayedGameCheckListener
    */
   public DelayedGameCheckListener getNewDelayedListener()
   {
      return new DelayedGameCheckListener(this.gameModel);
   }

   /*
    * This method returns a new RefreshCompCardListener
    */
   public RefreshCompCardListener getRefreshCompCardListener(Card computerCard)
   {
      return new RefreshCompCardListener(computerCard, this);
   }

   /*
    * This method returns a new HandCardMouseListener
    */
   public HandCardMouseListener getHandCardListener()
   {
      return new HandCardMouseListener(1, this.highCardGame, this.gameModel);
   }

   /*
    * An action listener for delayed code to execute checking logic after a nice
    * delay to a player is able to see game logic and cards change (also helper to
    * understand who wins).
    */
   static class DelayedGameCheckListener implements ActionListener
   {

      private final Model gameModel;

      /*
       * Constructor method for class DelayedGameCheckListener
       */
      public DelayedGameCheckListener(Model gameModel)
      {
         this.gameModel = gameModel;
      }

      /*
       * This method determines the primary action of the DelayedGameCheckListener
       */
      @Override
      public void actionPerformed(ActionEvent e)
      {
         gameModel.afterAllMoved();
      }
   }

   /*
    * An action listener for delayed code to show computer's played card with a
    * nice delay.
    */
   static class RefreshCompCardListener implements ActionListener
   {

      private final Card computerCard;
      private Controller gameController;

      /*
       * Constructor method for RefreshCompCardListener
       */
      public RefreshCompCardListener(Card computerCard, Controller newController)
      {
         this.computerCard = computerCard;
         this.gameController = newController;
      }

      /*
       * This method determines the primary action of the DelayedGameCheckListener
       */
      @Override
      public void actionPerformed(ActionEvent e)
      {
         gameController.requestSetPlayedCard(0, computerCard);
      }
   }

   /*
    * A mouse listener for each card on a players hand. Contains index of the card
    * and a reference to CardGameFramework.
    */
   static class HandCardMouseListener implements MouseListener
   {

      private final int cardIndex;
      private Model gameModel;

      /*
       * Constructor method for class HandCardMouseListener
       */
      public HandCardMouseListener(int cardIndex, CardGameFramework game, Model gameModel)
      {
         this.cardIndex = cardIndex;
         this.gameModel = gameModel;
      }

      /*
       * This method handles a mouse click event.
       */
      @Override
      public void mouseClicked(MouseEvent e)
      {
         this.gameModel.makeHumanMove(cardIndex);
      }

      /*
       * This method stub required by interface.
       */
      @Override
      public void mouseEntered(MouseEvent e)
      {
      }

      /*
       * This method stub required by interface.
       */
      @Override
      public void mouseExited(MouseEvent e)
      {
      }

      /*
       * This method stub required by interface.
       */
      @Override
      public void mousePressed(MouseEvent e)
      {
      }

      /*
       * This method stub required by interface.
       */
      @Override
      public void mouseReleased(MouseEvent e)
      {
      }
   }

   class GameButtonListener implements ActionListener
   {

      Controller gameController;

      public GameButtonListener(Controller gameController)
      {
         super();
         this.gameController = gameController;
      }

      /*
       * This method is the ActionListener for class CardTable. It contains the logic
       * for when the exitButton and newGameButton are pressed.
       */
      @Override
      public void actionPerformed(ActionEvent e)
      {
         String buttonString = e.getActionCommand();

         if (buttonString.equals("Exit Game"))
         {
            System.out.println("Ending Game.");
            System.exit(0);
         } else if (buttonString.equals("I Cannot Play"))
         {
            System.out.println("I Cannot Play Was Pressed");
         } else if (buttonString.equals("Start New Game"))
         {
            gameController.requestResetPlayedCards();
            gameController.setLastPlayedCard(null);
            gameController.setComputerWinner(false);
            gameController.setPlayerWinner(false);
            gameController.requestResetScores();
            gameController.requestScoreRedraw();
            highCardGame.newGame();
            highCardGame.deal();
            gameView.redrawPlayerHand();

         } else
            System.out.println("Unexpected Button Error.");
      }
   }
}

/*
 * One object of class GUICard represents a standard playing Card with a
 * graphical representation.
 */
class GUICard
{
   private static Icon[][] iconCards = new ImageIcon[14][4];
   private static Icon iconBack;
   private static boolean iconsLoaded = false;

   /*
    * This method returns an icon for the given Card object.
    */
   public static Icon getIcon(Card card)
   {
      GUICard.loadCardIcons();
      return iconCards[turnCardValueIntoInt(card)][turnCardSuitIntoInt(card)];
   }

   /*
    * This method returns an icon for the back of a playing card.
    */
   public static Icon getBackCardIcon()
   {
      GUICard.loadCardIcons();
      return iconBack;
   }

   /*
    * This method receives an integer and converts it to a string that represents
    * the value of a Card.
    */
   static String turnIntIntoCardValue(int k)
   {
      // test parameter validity
      if (k < 0 || k > 13)
      {
         return "";
      }

      String str = "";
      switch (k)
      {
      case 0:
         str = "A";
         break;
      case 1:
         str = "2";
         break;
      case 2:
         str = "3";
         break;
      case 3:
         str = "4";
         break;
      case 4:
         str = "5";
         break;
      case 5:
         str = "6";
         break;
      case 6:
         str = "7";
         break;
      case 7:
         str = "8";
         break;
      case 8:
         str = "9";
         break;
      case 9:
         str = "T";
         break;
      case 10:
         str = "J";
         break;
      case 11:
         str = "Q";
         break;
      case 12:
         str = "K";
         break;
      case 13:
         str = "X";
         break;
      }
      return str;
   }

   /*
    * This method receives an integer and converts it to the appropriate Suit value
    * for a Card.
    */
   static String turnIntIntoCardSuit(int j)
   {
      // test parameter validity
      if (j < 0 || j > 3)
      {
         return "";
      }
      String str = "";
      switch (j)
      {
      case 0:
         str = "C";
         break;
      case 1:
         str = "D";
         break;
      case 2:
         str = "H";
         break;
      case 3:
         str = "S";
         break;
      }
      return str;

   }

   /*
    * This method receives a Card object and returns it's value as an integer.
    */
   static int turnCardValueIntoInt(Card card)
   {
      // test parameter validity
      if (card == null)
      {
         return -1;
      }

      int number = -1;
      switch (card.getValue())
      {
      case 'A':
         number = 0;
         break;
      case '2':
         number = 1;
         break;
      case '3':
         number = 2;
         break;
      case '4':
         number = 3;
         break;
      case '5':
         number = 4;
         break;
      case '6':
         number = 5;
         break;
      case '7':
         number = 6;
         break;
      case '8':
         number = 7;
         break;
      case '9':
         number = 8;
         break;
      case 'T':
         number = 9;
         break;
      case 'J':
         number = 10;
         break;
      case 'Q':
         number = 11;
         break;
      case 'K':
         number = 12;
         break;
      case 'X':
         number = 13;
         break;
      }
      return number;
   }

   /*
    * This method receives a Card object and returns an integer representing the
    * Card's Suit.
    */
   static int turnCardSuitIntoInt(Card card)
   {
      // test parameter validity
      if (card == null)
      {
         return -1;
      }
      int number = -1;
      switch (card.getSuit())
      {
      case SPADES:
         number = 0;
         break;
      case HEARTS:
         number = 1;
         break;
      case DIAMONDS:
         number = 2;
         break;
      case CLUBS:
         number = 3;
         break;
      }
      return number;
   }

   /*
    * This method receives a String and returns the corresponding Suit value.
    */
   public static Card.Suit turnStringToSuit(String suit)
   {

      switch (suit.charAt(0))
      {
      case 'C':
         return Card.Suit.CLUBS;
      case 'D':
         return Card.Suit.DIAMONDS;
      case 'H':
         return Card.Suit.HEARTS;
      default:
         return Card.Suit.SPADES;
      }
   }

   /*
    * This method loads the Card Object images if they have not been done so
    * already.
    */
   static void loadCardIcons()
   {

      // check if already loaded
      if (iconsLoaded)
      {
         return;
      }

      String folder = "images/";
      String exten = ".gif";

      // generate card names and load icon
      for (int i = 0; i < iconCards.length; i++)
      {
         for (int j = 0; j < iconCards[0].length; j++)
         {
            iconCards[i][j] = new ImageIcon(folder + turnIntIntoCardValue(i) + turnIntIntoCardSuit(j) + exten);
         }
      }

      iconBack = new ImageIcon(folder + "BK" + exten);
      iconsLoaded = true;
   }
}

/*
 * One object of class Card represents a playing card complete with a value and
 * suit.
 */
class Card
{
   public enum Suit
   {
      CLUBS, SPADES, HEARTS, DIAMONDS
   }

   // Public Static Data Members:
   public static char[] valueRanks = new char[]
         { 'A', '2', '3', '4', '5', '6', '7', '8', '9', 'T', 'J', 'Q', 'K', 'X' };

   // Private Data Members:
   public char value;
   public Suit suit = null;
   private boolean errorFlag = false;

   /*
    * Constructor method for class Card. This method receives a value and suit and
    * creates a Card object with those attributes.
    */
   public Card(char value, Suit suit)
   {
      set(value, suit);
   }

   /*
    * Default constructor for class Card. Creates a Card that represents the ace of
    * spades.
    */
   public Card()
   {
      this('A', Suit.SPADES);
   }

   /*
    * Copy Constructor for class Card. This method receives a Card object, and
    * generates another one containing identical data.
    */
   public Card(Card card)
   {
      if (card == null)
      {
         return;
      }
      this.value = card.value;
      this.suit = card.suit;
   }

   /*
    * This method returns a string representation of the Card object.
    */
   @Override
   public String toString()
   {
      if (errorFlag)
      {
         return "**invalid**";
      } else
         return getValue() + " of " + getSuit();
   }

   /*
    * Mutator method that sets the numerical value and suit for the Card object.
    */
   public boolean set(char value, Suit suit)
   {
      if (isValid(value, suit))
      {
         this.value = Character.toUpperCase(value);
         this.suit = suit;
         this.errorFlag = false;
      } else
      {
         this.errorFlag = true;
      }
      return this.errorFlag;
   }

   /*
    * This method receives a boolean and sets the errorFlag to it.
    */
   public void setErrorFlag(boolean arg)
   {
      this.errorFlag = arg;
   }

   /*
    * This method returns the value for the card object.
    */
   public char getValue()
   {
      return this.value;
   }

   /*
    * This method returns the suit for the instance of the card.
    */
   public Suit getSuit()
   {
      return suit;
   }

   /*
    * This method returns the errorFlag boolean for the Card object.
    */
   public boolean getErrorFlag()
   {
      return errorFlag;
   }

   /*
    * This method receives both a char and Suit. The method then determines if they
    * are appropriate for Card creation and returns a boolean.
    */
   private boolean isValid(char value, Suit suit)
   {
      char upper = Character.toUpperCase(value);
      for (int i = 0; i < valueRanks.length; i++)
      {
         if (upper == valueRanks[i])
         {
            return true;
         }
      }
      return false;
   }

   /*
    * This method receives a Card object and determines if it is equal in value to
    * the current Card.
    */
   public boolean equals(Card card)
   {
      if (card == null)
      {
         return false;
      }
      return (this.value == card.value && this.suit == card.suit && this.errorFlag == card.errorFlag);
   }

   /*
    * This method receives an array of Card objects and sorts them based on their
    * Suit and value.
    */
   static void arraySort(Card[] cards, int arraySize)
   {
      Card tempCard;

      for (int i = 0; i < arraySize; i++)
      {
         for (int j = 1; j < arraySize - i; j++)
         {
            if (cardValue(cards[j - 1]) > cardValue(cards[j]))
            {
               tempCard = cards[j - 1];
               cards[j - 1] = cards[j];
               cards[j] = tempCard;
            }
         }
      }
   }

   /*
    * This methods receives Card object and returns a suit value to help with
    * cardvalue
    */
   public static int getSuitValue(Card cards)
   {
      if (cards == null)
      {
         return -1;
      }
      int number = -1;
      switch (cards.getSuit())
      {
      case SPADES:
         number = 0;
         break;
      case HEARTS:
         number = 14;
         break;
      case DIAMONDS:
         number = 28;
         break;
      case CLUBS:
         number = 42;
         break;
      }
      return number;
   }

   /*
    * This method receives a Card and returns an integer in comparison to the
    * current Card.
    */
   public static int cardValue(Card card)
   {

      return GUICard.turnCardValueIntoInt(card) + Card.getSuitValue(card);

   }

   /*
    * This method receives a Card object and compares the Suit and Value to the
    * receiving Card to determine which is of greater value. Will be used in game.
    */
   public int compareTo(Card card)
   {

      if (this.value == card.value)
      {
         return GUICard.turnCardSuitIntoInt(this) - GUICard.turnCardSuitIntoInt(card);
      }

      return GUICard.turnCardValueIntoInt(this) - GUICard.turnCardValueIntoInt(card);
   }
}

/*
 * One object of class Hand represents a hand in a card game. It is composed of
 * Card objects.
 */
class Hand
{
   public static final int MAX_CARDS = 56;
   private Card[] myCards;
   private int numCards;

   /*
    * This method is the default constructor method for class Hand.
    */
   public Hand()
   {
      this.numCards = 0;
      this.myCards = new Card[MAX_CARDS];
   }

   /*
    * This method moves all the card objects from the Hand object.
    */
   public void resetHand()
   {
      for (int i = 0; i < numCards; i++)
      {
         this.myCards[i] = null;
      }
      this.numCards = 0;
   }

   /*
    * This method adds a Card object to the next slot in the Hand object.
    */
   public boolean takeCard(Card card)
   {
      if (card == null)
         return false;

      // make a copy
      Card cardCopy = new Card(card);
      if (this.numCards < this.myCards.length)
      {
         this.myCards[this.numCards] = cardCopy;
         this.numCards++;
         return true;
      }
      return false;
   }

   /*
    * This method returns the top card from the Hand object and removes it.
    */
   public Card playCard()
   {
      Card returnCard = this.myCards[this.numCards - 1];
      this.myCards[this.numCards - 1] = null;
      this.numCards--;
      return returnCard;
   }

   /*
    * This method returns the value of numCards for the Hand object.
    */
   public int getNumCards()
   {
      return this.numCards;
   }

   /*
    * This method creates a string representation of the Hand object.
    */
   @Override
   public String toString()
   {
      String returnString = "(";
      for (int i = 0; i < this.numCards; i++)
      {
         if (i == this.numCards - 1)
         {
            returnString += this.myCards[i].toString();
         } else
            returnString += this.myCards[i].toString() + ", ";
      }
      returnString += ")";
      return returnString;
   }

   /*
    * This method receives an integer value and locates the associated Card in the
    * Hand. If the Card is not located, a new Card is generated and returned.
    */
   public Card inspectCard(int k)
   {
      if (k >= 0 && k < numCards)
         return myCards[k];
      else
         return new Card('Z', Card.Suit.SPADES);
   }

   /*
    * This method receives a index value and returns the appropriate Card from the
    * Hand.
    */
   public Card playCard(int cardIndex)
   {
      if (numCards == 0) // error
      {
         // Creates a card that does not work
         return new Card('Z', Card.Suit.SPADES);
      }
      // Decreases numCards.
      Card card = myCards[cardIndex];

      numCards--;
      for (int i = cardIndex; i < numCards; i++)
      {
         myCards[i] = myCards[i + 1];
      }

      myCards[numCards] = null;

      return card;
   }

   /*
    * This method sorts the Hand object. MM WR
    */
   public void sort()
   {
      Card.arraySort(myCards, myCards.length);
   }
}

/*
 * One object of class Deck represents a standard deck of playing cards.
 */
class Deck
{
   public static final int MAX_CARDS = 336;
   private static Card[] masterPack;
   private Card[] cards;
   private int topCard;

   /*
    * Constructor method for Class Deck. This method receives an integer numPacks
    * and generates a deck with numPacks number of card packs.
    */
   public Deck(int numPacks)
   {
      allocateMasterPack();
      init(numPacks);
   }

   /*
    * Default Constructor method for Class Deck. This method creates a Deck with
    * one pack of Card objects.
    */
   public Deck()
   {
      this(1);
   }

   /*
    * This method repopulates the cards in the Cards private member.
    */
   public void init(int numPacks)
   {
      int packLimit = (MAX_CARDS / 56);
      // array initialized with total number of cards
      if (numPacks > 0 && numPacks <= packLimit)
      {
         int total = numPacks * 56;
         cards = new Card[total];
         for (int i = 0; i < cards.length; i++)
         {
            cards[i] = new Card(masterPack[i % masterPack.length]);
         }
         this.topCard = total; // initialize topCard with the total amount
      }
   }

   /*
    * This method generates the master pack for a deck of Card objects. Updated to
    * include Joker.
    */
   private static void allocateMasterPack()
   {
      // check if masterPack has already been generated.
      if (masterPack != null)
         return;

      masterPack = new Card[56];
      int count = 0;
      char[] values =
         { 'T', 'J', 'Q', 'K', 'A', 'X' };

      // make all the numbered cards
      for (char i = '2'; i <= '9'; i++)
      {
         for (Card.Suit suitType : Card.Suit.values())
         {
            Card newCard = new Card(i, suitType);
            masterPack[count] = newCard;
            count++;
         }
      }

      // make all the face cards
      for (char value : values)
      {
         for (Card.Suit suitType : Card.Suit.values())
         {
            Card newCard = new Card(value, suitType);
            masterPack[count] = newCard;
            count++;
         }
      }
   }

   /*
    * This method mixes up the cards contained in the Deck object.
    */
   public void shuffle()
   {
      int index1;
      int index2;
      int num = cards.length;
      while (num > 0)
      {
         index1 = (int) (Math.random() * cards.length);
         index2 = (int) (Math.random() * cards.length);

         // swapping the elements
         Card temp = cards[index1];
         cards[index1] = cards[index2];
         cards[index2] = temp;
         num--;
      }
   }

   /*
    * This method returns the Card object from the top of the deck.
    */
   public Card dealCard()
   {
      if (this.topCard <= 0)
         return null;
      Card returnCard = this.cards[this.topCard - 1];
      this.cards[this.topCard - 1] = null;
      this.topCard--;
      return returnCard;
   }

   /*
    * This method returns the value for the requested Card object in the Deck.
    */
   public Card inspectCard(int k)
   {
      if (k >= 0 && k < cards.length)
         return cards[k];
      else
         return new Card('Z', Card.Suit.SPADES);
   }

   /*
    * This method returns the object value for topCard.
    */
   public int getTopCard()
   {
      return this.topCard;
   }

   /*
    * This method sorts the Deck of Cards.
    */
   public void sort()
   {
      Card.arraySort(cards, topCard);
   }

   /*
    * This method receives a Card and removes the matching Card from the Deck.
    */
   public boolean removeCard(Card card)
   {
      // test parameter validity
      if (card == null)
      {
         return false;
      }

      boolean found = false;

      for (int i = 0; i < topCard; i++)
      {
         if (cards[i].equals(card))
         {
            cards[i] = cards[topCard - 1];
            topCard--;
            found = true;
            break;
         }
      }
      return found;
   }

   /*
    * This method returns an integer containing the number of Cards in the Deck.
    */
   public int getNumCards()
   {
      return topCard;
   }

   /*
    * This method adds a Card object to the Deck. A boolean is returned describing
    * the status of completion.
    */
   public boolean addCard(Card card)
   {
      // test parameter validity
      if (card == null)
      {
         return false;
      }

      // check the space to add a new card
      if (topCard == MAX_CARDS)
      {
         return false;
      }

      int countCopies = 0;
      int packLimit = (MAX_CARDS / 56);
      // check number of copies of the card
      for (int i = 0; i < topCard; i++)
      {
         if (cards[i].equals(card))
         {
            countCopies++;
         }
      }

      if (countCopies >= packLimit)
      {
         return false;
      }

      topCard++;
      cards[topCard - 1] = card;
      return true;
   }
}

//class CardGameFramework  ----------------------------------------------------
class CardGameFramework
{
   private static final int MAX_PLAYERS = 50;

   private int numPlayers;
   private int numPacks; // # standard 52-card packs per deck
   // ignoring jokers or unused cards
   private int numJokersPerPack; // if 2 per pack & 3 packs per deck, get 6
   private int numUnusedCardsPerPack; // # cards removed from each pack
   private int numCardsPerHand; // # cards to deal each player
   private Deck deck; // holds the initial full deck and gets
   // smaller (usually) during play
   private Hand[] hand; // one Hand for each player
   private Card[] unusedCardsPerPack; // an array holding the cards not used
   // in the game. e.g. pinochle does not
   // use cards 2-8 of any suit

   public CardGameFramework(int numPacks, int numJokersPerPack, int numUnusedCardsPerPack, Card[] unusedCardsPerPack,
         int numPlayers, int numCardsPerHand)
   {
      int k;

      // filter bad values
      if (numPacks < 1 || numPacks > 6)
         numPacks = 1;
      if (numJokersPerPack < 0 || numJokersPerPack > 4)
         numJokersPerPack = 0;
      if (numUnusedCardsPerPack < 0 || numUnusedCardsPerPack > 50) // > 1 card
         numUnusedCardsPerPack = 0;
      if (numPlayers < 1 || numPlayers > MAX_PLAYERS)
         numPlayers = 4;
      // one of many ways to assure at least one full deal to all players
      if (numCardsPerHand < 1 || numCardsPerHand > numPacks * (52 - numUnusedCardsPerPack) / numPlayers)
         numCardsPerHand = numPacks * (52 - numUnusedCardsPerPack) / numPlayers;

      // allocate
      this.unusedCardsPerPack = new Card[numUnusedCardsPerPack];
      this.hand = new Hand[numPlayers];
      for (k = 0; k < numPlayers; k++)
         this.hand[k] = new Hand();
      deck = new Deck(numPacks);

      // assign to members
      this.numPacks = numPacks;
      this.numJokersPerPack = numJokersPerPack;
      this.numUnusedCardsPerPack = numUnusedCardsPerPack;
      this.numPlayers = numPlayers;
      this.numCardsPerHand = numCardsPerHand;
      for (k = 0; k < numUnusedCardsPerPack; k++)
         this.unusedCardsPerPack[k] = unusedCardsPerPack[k];

      // prepare deck and shuffle
      newGame();
   }

   // constructor overload/default for game like bridge
   public CardGameFramework()
   {
      this(1, 0, 0, null, 4, 13);
   }

   public Hand getHand(int k)
   {
      // hands start from 0 like arrays

      // on error return automatic empty hand
      if (k < 0 || k >= numPlayers)
         return new Hand();

      return hand[k];
   }

   public Card getCardFromDeck()
   {
      return deck.dealCard();
   }

   public int getNumCardsRemainingInDeck()
   {
      return deck.getNumCards();
   }

   public void newGame()
   {
      int k, j;

      // clear the hands
      for (k = 0; k < numPlayers; k++)
         hand[k].resetHand();

      // restock the deck
      deck.init(numPacks);

      // remove unused cards
      for (k = 0; k < numUnusedCardsPerPack; k++)
         deck.removeCard(unusedCardsPerPack[k]);

      // add jokers
      for (k = 0; k < numPacks; k++)
         for (j = 0; j < numJokersPerPack; j++)
            deck.addCard(new Card('X', Card.Suit.values()[j]));

      // shuffle the cards
      deck.shuffle();
   }

   public boolean deal()
   {
      // returns false if not enough cards, but deals what it can
      int k, j;
      boolean enoughCards;

      // clear all hands
      for (j = 0; j < numPlayers; j++)
         hand[j].resetHand();

      enoughCards = true;
      for (k = 0; k < numCardsPerHand && enoughCards; k++)
      {
         for (j = 0; j < numPlayers; j++)
            if (deck.getNumCards() > 0)
               hand[j].takeCard(deck.dealCard());
            else
            {
               enoughCards = false;
               break;
            }
      }

      return enoughCards;
   }

   void sortHands()
   {
      int k;

      for (k = 0; k < numPlayers; k++)
         hand[k].sort();
   }

   Card playCard(int playerIndex, int cardIndex)
   {
      // returns bad card if either argument is bad
      if (playerIndex < 0 || playerIndex > numPlayers - 1 || cardIndex < 0 || cardIndex > numCardsPerHand - 1)
      {
         // Creates a card that does not work
         return new Card('Z', Card.Suit.SPADES);
      }

      // return the card played
      return hand[playerIndex].playCard(cardIndex);

   }

   boolean takeCard(int playerIndex)
   {
      // returns false if either argument is bad
      if (playerIndex < 0 || playerIndex > numPlayers - 1)
         return false;

      // Are there enough Cards?
      if (deck.getNumCards() <= 0)
         return false;

      return hand[playerIndex].takeCard(deck.dealCard());
   }
}

/*
 * class TimerModel extends Thread { private int minutes, seconds; private
 * boolean timerOn, timerStarted; private final static int WAIT = 1500; private
 * final TimerDisplay display;
 * 
 * public TimerModel(TimerDisplay displayIn) { minutes = 0; seconds = 0; timerOn
 * = false; display = displayIn; }
 * 
 * public void run() { timerStarted = true; while(timerStarted) {
 * doNothing(WAIT); if(seconds < 59 && timerOn) { seconds++; }
 * 
 * else if((seconds >= 59) && timerOn) { seconds = 0; minutes++; } if(timerOn) {
 * printTime(); display.update(this); } } }
 * 
 * public void stopTimer() { timerOn = false; }
 * 
 * public void resumeTimer() { timerOn = true; }
 * 
 * public boolean timerOn() { return timerOn; }
 * 
 * public int getMinutes() { return minutes; }
 * 
 * public int getSeconds() { return seconds; }
 * 
 * public int getMS() { return (minutes * 60000) + (seconds *1000); }
 * 
 * public boolean started() { return timerStarted; }
 * 
 * public TimerDisplay getDisplayObject() { return display; }
 * 
 * public String toString() { if(seconds < 10) return String.format(" %s:0%s ",
 * minutes, seconds); else return String.format(" %s:0%s ", minutes, seconds); }
 * 
 * private void printTime() { System.out.printf(toString()); }
 * 
 * private void doNothing(int waitTime) { try { Thread.sleep(waitTime); }
 * catch(InterruptedException e) { e.printStackTrace(); System.exit(0); } } }
 */