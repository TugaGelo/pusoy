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

    // Constructor to create a new Player object.
    public Player(String name) {
        // We'll generate a unique ID for each player.
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.hand = null; // The hand is set later when cards are dealt.
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
}
