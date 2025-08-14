package com.pusoygame.pusoybackend;

import java.util.UUID;

// The Player class represents a single participant in the game.
public class Player {

    // A unique identifier for the player.
    private String id;
    // The player's name.
    private String name;
    // The player's current hand, which will hold 13 cards.
    private Hand hand;

    // The three hands a player forms from their 13 cards.
    private Hand frontHand;
    private Hand middleHand;
    private Hand backHand;

    // Constructor to create a new Player object.
    public Player(String name) {
        // We'll generate a unique ID for each player.
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.hand = null; // The hand is set later when cards are dealt.
        this.frontHand = null;
        this.middleHand = null;
        this.backHand = null;
    }

    // This is the new method we're adding.
    // It's a key part of the game where a player arranges their cards.
    public void setHands(Hand frontHand, Hand middleHand, Hand backHand) {
        // You would also need logic here to ensure that the hands
        // are a valid 3, 5, and 5 card combination and that the
        // poker hand rankings are correct (back > middle > front).
        this.frontHand = frontHand;
        this.middleHand = middleHand;
        this.backHand = backHand;
    }

    // Getter methods to access the player's properties.
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Hand getHand() {
        return hand;
    }

    // Setter method to give the player a new hand of cards.
    public void setHand(Hand hand) {
        this.hand = hand;
    }

    public Hand getFrontHand() {
        return frontHand;
    }

    public Hand getMiddleHand() {
        return middleHand;
    }

    public Hand getBackHand() {
        return backHand;
    }
}
