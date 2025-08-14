package com.pusoygame.pusoybackend;

import java.util.List;

public class HandSubmission {
    
    private String playerId;

    private List<Card> frontHand;
    private List<Card> middleHand;
    private List<Card> backHand;

    public String getPlayerId() {
        return playerId;
    }

    public List<Card> getFrontHand() {
        return frontHand;
    }

    public List<Card> getMiddleHand() {
        return middleHand;
    }

    public List<Card> getBackHand() {
        return backHand;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public void setFrontHand(List<Card> frontHand) {
        this.frontHand = frontHand;
    }

    public void setMiddleHand(List<Card> middleHand) {
        this.middleHand = middleHand;
    }

    public void setBackHand(List<Card> backHand) {
        this.backHand = backHand;
    }
}
