package com.pusoygame.pusoybackend;

import java.util.List;

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
}
