package com.pusoygame.pusoybackend;

import java.util.UUID;

// The Player class represents a single participant in the game.
public class Player {

    private String id;
    private String name;
    private Hand hand;

    private Hand frontHand;
    private Hand middleHand;
    private Hand backHand;

    // Constructor to create a new Player object.
    public Player(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.hand = null;
        this.frontHand = null;
        this.middleHand = null;
        this.backHand = null;
    }

    public void setHands(Hand frontHand, Hand middleHand, Hand backHand) {
        this.frontHand = frontHand;
        this.middleHand = middleHand;
        this.backHand = backHand;

        // Logging builds to console whenever hands are set
        System.out.println("Player: " + name);
        if (frontHand != null) {
            System.out.println("  Front Hand:  " + frontHand + " (" + HandEvaluator.getHandName(frontHand) + ")");
        }
        if (middleHand != null) {
            System.out.println("  Middle Hand: " + middleHand + " (" + HandEvaluator.getHandName(middleHand) + ")");
        }
        if (backHand != null) {
            System.out.println("  Back Hand:   " + backHand + " (" + HandEvaluator.getHandName(backHand) + ")");
        }
        System.out.println("-------------------------------------------------");
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Hand getHand() {
        return hand;
    }

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
