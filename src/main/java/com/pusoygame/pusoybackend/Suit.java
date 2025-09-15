package com.pusoygame.pusoybackend;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Suit {
    HEARTS("♥", "red"),
    DIAMONDS("♦", "red"),
    CLUBS("♣", "black"),
    SPADES("♠", "black");

    private final String symbol;
    private final String color;

    Suit(String symbol, String color) {
        this.symbol = symbol;
        this.color = color;
    }

    @JsonValue
    public String getSymbol() {
        return symbol;
    }

    public String getColor() {
        return color;
    }
}
