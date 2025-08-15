package com.pusoygame.pusoybackend;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

// The Game class manages the game state, including the deck and the players.
public class Game {

    private List<Card> deck;
    private List<Player> players;
    private int currentPlayerIndex;

    // Constructor to set up a new game with a list of players.
    public Game(List<Player> players) {
        // System.out.println("Game constructor called. Creating a new game with " + players.size() + " players.");
        this.players = players;
        this.deck = new ArrayList<>();
        initializeDeck();
        shuffleDeck();
        this.currentPlayerIndex = 0;
        dealCards();
    }

    // Creates a standard 52-card deck.
    private void initializeDeck() {
        // System.out.println("Initializing a new 52-card deck...");
        String[] suits = {"Hearts", "Diamonds", "Clubs", "Spades"};
        for (String suit : suits) {
            for (int rank = 2; rank <= 10; rank++) {
                deck.add(new Card(suit, rank));
            }
            deck.add(new Card(suit, 11));
            deck.add(new Card(suit, 12));
            deck.add(new Card(suit, 13));
            deck.add(new Card(suit, 14));
        }
        // System.out.println("Deck initialized with " + deck.size() + " cards: " + deck);
    }

    private void shuffleDeck() {
        // System.out.println("Shuffling the deck.");
        Collections.shuffle(deck);
        // System.out.println("Deck after shuffling: " + deck);
    }

    // Deals 13 cards to each player.
    private void dealCards() {
        // System.out.println("Dealing 13 cards to each of the " + players.size() + " players.");
        for (Player player : players) {
            List<Card> playerCardList = new ArrayList<>();
            for (int i = 0; i < 13; i++) {
                playerCardList.add(deck.remove(0));
            }
            player.setHand(new Hand(playerCardList));
            // System.out.println("Player " + player.getName() + " was dealt " + player.getHand().getCards().size() + " cards: " + player.getHand().getCards());
        }
    }
    
    // A method to sort the cards in a player's hand.
    public void sortPlayerHand(Player player) {
        if (player != null && player.getHand() != null) {
            // System.out.println("Sorting " + player.getName() + "'s hand by rank. Before sorting: " + player.getHand().getCards());
            Collections.sort(player.getHand().getCards(), Comparator.comparingInt(Card::getRank));
            // System.out.println("Sorting " + player.getName() + "'s hand by rank. After sorting: " + player.getHand().getCards());
        }
    }

    // A method checks if a player's three hands are in the correct order to avoid a foul.
    public boolean checkFoul(Hand front, Hand middle, Hand back) {
        // System.out.println("Checking for a foul. Comparing hands...");
        boolean backIsStrongerThanMiddle = HandEvaluator.compareHands(back, middle) > 0;
        boolean middleIsStrongerThanFront = HandEvaluator.compareHands(middle, front) > 0;
        
        // if (!(backIsStrongerThanMiddle && middleIsStrongerThanFront)) {
        //     System.out.println("Foul detected! Back hand is not stronger than middle hand, or middle hand is not stronger than front hand.");
        // }

        return !(backIsStrongerThanMiddle && middleIsStrongerThanFront);
    }

    // A method to set a player's three hands and check for a foul.
    public boolean setPlayerHands(Player player, Hand front, Hand middle, Hand back) {
        // System.out.println("Attempting to set hands for " + player.getName() + ".");
        if (checkFoul(front, middle, back)) {
            return false;
        }
        
        // System.out.println("Hands are valid. Setting hands for " + player.getName() + ".");
        // System.out.println("Front Hand: " + front.getCards());
        // System.out.println("Middle Hand: " + middle.getCards());
        // System.out.println("Back Hand: " + back.getCards());
        player.setHands(front, middle, back);
        return true;
    }

    // A method to set the hands for the AI players.
    public void setAIHands(Player player) {
        // System.out.println("Setting AI hands for " + player.getName() + ".");
        player.getHand().sortCardsByRank();
        List<Card> cards = new ArrayList<>(player.getHand().getCards());

        // Strategy 1: Greedy Approach
        Hand backHand = new Hand(cards.subList(8, 13));
        Hand middleHand = new Hand(cards.subList(3, 8));
        Hand frontHand = new Hand(cards.subList(0, 3));

        if (setPlayerHands(player, frontHand, middleHand, backHand)) {
            System.out.println(player.getName() + " set their hands without a foul using Strategy 1.");
            return;
        }

        // Strategy 2: If greedy fouls, try an alternative arrangement.
        Hand alternativeBackHand = new Hand(cards.subList(8, 13));
        Hand alternativeFrontHand = new Hand(cards.subList(3, 8));
        Hand alternativeMiddleHand = new Hand(cards.subList(0, 3));
        
        if (setPlayerHands(player, alternativeFrontHand, alternativeMiddleHand, alternativeBackHand)) {
            System.out.println(player.getName() + " set their hands without a foul using Strategy 2.");
            return;
        }

        // Strategy 3: Default fallback.
        player.setHands(new Hand(cards.subList(0, 3)), new Hand(cards.subList(3, 8)), new Hand(cards.subList(8, 13)));
        System.out.println(player.getName() + " fouled! They will lose this round but their hands are now set with the default fallback.");
    }
    
    // A method to compare the human player's hands to each AI's hands individually.
    public void compareAllPlayerHands() {
        System.out.println("\n--- Starting the Showdown ---");
        
        Player humanPlayer = players.get(0);

        for (int i = 1; i < players.size(); i++) {
            Player aiPlayer = players.get(i);
            
            if (humanPlayer.getBackHand() != null && aiPlayer.getBackHand() != null) {
                System.out.println("Comparing hands for " + humanPlayer.getName() + " vs " + aiPlayer.getName() + ":");

                int backComparison = HandEvaluator.compareHands(humanPlayer.getBackHand(), aiPlayer.getBackHand());
                if (backComparison > 0) {
                    System.out.println("- " + humanPlayer.getName() + "'s back hand wins! (Hand: " + humanPlayer.getBackHand().getCards() + ")");
                } else if (backComparison < 0) {
                    System.out.println("- " + aiPlayer.getName() + "'s back hand wins! (Hand: " + aiPlayer.getBackHand().getCards() + ")");
                } else {
                    System.out.println("- Back hands are a tie!");
                }

                int middleComparison = HandEvaluator.compareHands(humanPlayer.getMiddleHand(), aiPlayer.getMiddleHand());
                if (middleComparison > 0) {
                    System.out.println("- " + humanPlayer.getName() + "'s middle hand wins! (Hand: " + humanPlayer.getMiddleHand().getCards() + ")");
                } else if (middleComparison < 0) {
                    System.out.println("- " + aiPlayer.getName() + "'s middle hand wins! (Hand: " + aiPlayer.getMiddleHand().getCards() + ")");
                } else {
                    System.out.println("- Middle hands are a tie!");
                }

                int frontComparison = HandEvaluator.compareHands(humanPlayer.getFrontHand(), aiPlayer.getFrontHand());
                if (frontComparison > 0) {
                    System.out.println("- " + humanPlayer.getName() + "'s front hand wins! (Hand: " + humanPlayer.getFrontHand().getCards() + ")");
                } else if (frontComparison < 0) {
                    System.out.println("- " + aiPlayer.getName() + "'s front hand wins! (Hand: " + aiPlayer.getFrontHand().getCards() + ")");
                } else {
                    System.out.println("- Front hands are a tie!");
                }
            }
        }
    }

    // --- Getter Methods ---
    public List<Player> getPlayers() {
        return players;
    }

    public List<Card> getDeck() {
        return deck;
    }
}
