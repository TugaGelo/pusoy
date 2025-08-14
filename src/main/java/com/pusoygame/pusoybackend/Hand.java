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
        this.cards = cards;
    }

    // Getter method to access the list of cards in the hand
    public List<Card> getCards() {
        return cards;
    }

    // A method to add a card to the hand
    public void addCard(Card card) {
        this.cards.add(card);
    }
    
    // A new method to sort the cards in the hand by rank.
    // This is useful for both displaying cards and for poker hand evaluation.
    public void sortCardsByRank() {
        this.cards.sort(Comparator.comparingInt(Card::getRank));
    }

    // This new helper method counts the number of times each card rank appears in the hand.
    // This is a crucial step for evaluating pairs, three-of-a-kind, and other combinations.
    public Map<Integer, Long> getRankCounts() {
        return this.cards.stream()
                .collect(Collectors.groupingBy(Card::getRank, Collectors.counting()));
    }
}
