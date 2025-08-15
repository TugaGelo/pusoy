package com.pusoygame.pusoybackend;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// The Hand class represents a collection of Card objects.
public class Hand {

    private List<Card> cards;

    // Constructor to create a new Hand object
    public Hand(List<Card> cards) {
        // System.out.println("Hand constructor called. Creating a new hand with " + cards.size());
        this.cards = cards;
    }

    // Getter method to access the list of cards in the hand
    public List<Card> getCards() {
        // System.out.println("getCards() method called. Returning the list of cards: " + this.cards);
        return cards;
    }

    // A method to add a card to the hand
    public void addCard(Card card) {
        // System.out.println("addCard() method called. Adding " + card + " to the hand.");
        this.cards.add(card);
    }
    
    // A new method to sort the cards in the hand by rank.
    public void sortCardsByRank() {
        // System.out.println("sortCardsByRank() called. Hand BEFORE sorting: " + this.cards);
        this.cards.sort(Comparator.comparingInt(Card::getRank));
        // System.out.println("sortCardsByRank() called. Hand AFTER sorting: " + this.cards);
    }

    // A method that counts the number of times each card rank appears in the hand.
    public Map<Integer, Long> getRankCounts() {
        // System.out.println("getRankCounts() called. Counting card ranks.");
        Map<Integer, Long> rankCounts = this.cards.stream()
                .collect(Collectors.groupingBy(Card::getRank, Collectors.counting()));
        // System.out.println("getRankCounts() returning result: " + rankCounts);
        return rankCounts;
    }
}
