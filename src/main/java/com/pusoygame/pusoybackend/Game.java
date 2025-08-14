package com.pusoygame.pusoybackend;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

// The Game class manages the game state, including the deck and the players.
public class Game {

    private List<Card> deck;
    private List<Player> players;
    private int currentPlayerIndex; // Index of the player whose turn it is.

    // Constructor to set up a new game with a list of players.
    public Game(List<Player> players) {
        this.players = players;
        this.deck = new ArrayList<>();
        // Initialize the deck with 52 cards.
        initializeDeck();
        // Shuffle the deck to randomize the card order.
        shuffleDeck();
        this.currentPlayerIndex = 0; // Start with the first player.
        // Deal 13 cards to each player.
        dealCards();
    }

    // Creates a standard 52-card deck.
    private void initializeDeck() {
        String[] suits = {"Hearts", "Diamonds", "Clubs", "Spades"};
        for (String suit : suits) {
            // Rank 2 to 10
            for (int rank = 2; rank <= 10; rank++) {
                deck.add(new Card(suit, rank));
            }
            // Rank 11 (Jack), 12 (Queen), 13 (King), 14 (Ace)
            deck.add(new Card(suit, 11)); // Jack
            deck.add(new Card(suit, 12)); // Queen
            deck.add(new Card(suit, 13)); // King
            deck.add(new Card(suit, 14)); // Ace
        }
    }

    // Shuffles the deck using Java's built-in Collections.shuffle method.
    private void shuffleDeck() {
        Collections.shuffle(deck);
    }

    // Deals 13 cards to each player.
    private void dealCards() {
        for (Player player : players) {
            List<Card> playerCardList = new ArrayList<>();
            for (int i = 0; i < 13; i++) {
                playerCardList.add(deck.remove(0)); // Take a card from the top of the deck.
            }
            player.setHand(new Hand(playerCardList)); // Give the player their hand.
        }
    }
    
    // A method to sort the cards in a player's hand.
    public void sortPlayerHand(Player player) {
        if (player != null && player.getHand() != null) {
            Collections.sort(player.getHand().getCards(), Comparator.comparingInt(Card::getRank));
        }
    }

    // This new method checks if a player's three hands are in the correct order to avoid a foul.
    // It uses the HandEvaluator to compare the hands.
    public boolean checkFoul(Hand front, Hand middle, Hand back) {
        // A foul occurs if the back hand is not stronger than the middle, or if the middle is not stronger than the front.
        // HandEvaluator.compareHands returns a positive value if the first hand is stronger.
        boolean backIsStrongerThanMiddle = HandEvaluator.compareHands(back, middle) > 0;
        boolean middleIsStrongerThanFront = HandEvaluator.compareHands(middle, front) > 0;

        return !(backIsStrongerThanMiddle && middleIsStrongerThanFront);
    }

    // A new method to set a player's three hands and check for a foul.
    // This is where the AI or a human player will "lock in" their hands.
    public boolean setPlayerHands(Player player, Hand front, Hand middle, Hand back) {
        if (checkFoul(front, middle, back)) {
            // If the hands are a foul, we don't set them and return false.
            return false;
        }
        
        // If the hands are valid, we set them and return true.
        player.setHands(front, middle, back);
        return true;
    }

    // --- Getter Methods ---
    public List<Player> getPlayers() {
        return players;
    }

    public List<Card> getDeck() {
        return deck;
    }
}
