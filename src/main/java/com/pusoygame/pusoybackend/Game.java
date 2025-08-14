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
        this.players = players;
        this.deck = new ArrayList<>();
        initializeDeck();
        shuffleDeck();
        this.currentPlayerIndex = 0;
        dealCards();
    }

    // Creates a standard 52-card deck.
    private void initializeDeck() {
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
    }

    private void shuffleDeck() {
        Collections.shuffle(deck);
    }

    // Deals 13 cards to each player.
    private void dealCards() {
        for (Player player : players) {
            List<Card> playerCardList = new ArrayList<>();
            for (int i = 0; i < 13; i++) {
                playerCardList.add(deck.remove(0));
            }
            player.setHand(new Hand(playerCardList));
        }
    }
    
    // A method to sort the cards in a player's hand.
    public void sortPlayerHand(Player player) {
        if (player != null && player.getHand() != null) {
            Collections.sort(player.getHand().getCards(), Comparator.comparingInt(Card::getRank));
        }
    }

    // A method checks if a player's three hands are in the correct order to avoid a foul.
    public boolean checkFoul(Hand front, Hand middle, Hand back) {
        boolean backIsStrongerThanMiddle = HandEvaluator.compareHands(back, middle) > 0;
        boolean middleIsStrongerThanFront = HandEvaluator.compareHands(middle, front) > 0;

        return !(backIsStrongerThanMiddle && middleIsStrongerThanFront);
    }

    // A method to set a player's three hands and check for a foul.
    public boolean setPlayerHands(Player player, Hand front, Hand middle, Hand back) {
        if (checkFoul(front, middle, back)) {
            return false;
        }
        
        player.setHands(front, middle, back);
        return true;
    }

    // A method to set the hands for the AI players.
    public void setAIHands(Player player) {
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

    // A method to compare all players' hands and determine the winner.
    public void compareAllPlayerHands() {
        System.out.println("\n--- Starting the Showdown ---");
        
        for (int i = 0; i < players.size(); i++) {
            for (int j = i + 1; j < players.size(); j++) {
                Player player1 = players.get(i);
                Player player2 = players.get(j);

                if (player1.getBackHand() != null && player2.getBackHand() != null) {
                    System.out.println("Comparing hands for " + player1.getName() + " vs " + player2.getName() + ":");

                    // Compare Back Hands
                    int backComparison = HandEvaluator.compareHands(player1.getBackHand(), player2.getBackHand());
                    if (backComparison > 0) {
                        System.out.println("- " + player1.getName() + "'s back hand wins!");
                    } else if (backComparison < 0) {
                        System.out.println("- " + player2.getName() + "'s back hand wins!");
                    } else {
                        System.out.println("- Back hands are a tie!");
                    }

                    // Compare Middle Hands
                    int middleComparison = HandEvaluator.compareHands(player1.getMiddleHand(), player2.getMiddleHand());
                    if (middleComparison > 0) {
                        System.out.println("- " + player1.getName() + "'s middle hand wins!");
                    } else if (middleComparison < 0) {
                        System.out.println("- " + player2.getName() + "'s middle hand wins!");
                    } else {
                        System.out.println("- Middle hands are a tie!");
                    }

                    // Compare Front Hands
                    int frontComparison = HandEvaluator.compareHands(player1.getFrontHand(), player2.getFrontHand());
                    if (frontComparison > 0) {
                        System.out.println("- " + player1.getName() + "'s front hand wins!");
                    } else if (frontComparison < 0) {
                        System.out.println("- " + player2.getName() + "'s front hand wins!");
                    } else {
                        System.out.println("- Front hands are a tie!");
                    }
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
