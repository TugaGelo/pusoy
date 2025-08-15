package com.pusoygame.pusoybackend;
public class Card {

    private String suit;
    private int rank;

    // Constructor to create a new Card object
    public Card(String suit, int rank) {
        // System.out.println("Card constructor called. Initializing with suit: " + suit + ", rank: " + rank);
        this.suit = suit;
        this.rank = rank;
    }

    public String getSuit() {
        // System.out.println("getSuit() called. Returning suit: " + suit);
        return suit;
    }

    public int getRank() {
        // System.out.println("getRank() called. Returning rank: " + rank);
        return rank;
    }

    // A method to get a readable string for the card
    @Override
    public String toString() {
        String rankStr;
        switch (rank) {
            case 11:
                rankStr = "Jack";
                break;
            case 12:
                rankStr = "Queen";
                break;
            case 13:
                rankStr = "King";
                break;
            case 14:
                rankStr = "Ace";
                break;
            default:
                rankStr = String.valueOf(rank);
        }
        return rankStr + " of " + suit;
    }
}
