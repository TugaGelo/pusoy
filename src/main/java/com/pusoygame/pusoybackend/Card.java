package com.pusoygame.pusoybackend;

// We'll use this class to represent a single playing card.
// It will have two properties: a suit and a rank.
public class Card {

    private String suit; // e.g., "Hearts", "Diamonds", "Clubs", "Spades"
    private int rank; // 2-14 (11=Jack, 12=Queen, 13=King, 14=Ace)

    // Constructor to create a new Card object
    public Card(String suit, int rank) {
        this.suit = suit;
        this.rank = rank;
    }

    // Getter methods to access the properties of the card
    public String getSuit() {
        return suit;
    }

    public int getRank() {
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
