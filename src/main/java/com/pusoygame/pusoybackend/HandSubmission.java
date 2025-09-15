package com.pusoygame.pusoybackend;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class HandSubmission {

    private String playerId;

    @JsonProperty("frontHand")
    private List<Card> frontHand;

    @JsonProperty("middleHand")
    private List<Card> middleHand;

    @JsonProperty("backHand")
    private List<Card> backHand;

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public List<Card> getFrontHand() {
        return frontHand;
    }

    public void setFrontHand(List<Card> frontHand) {
        this.frontHand = frontHand;
    }

    public List<Card> getMiddleHand() {
        return middleHand;
    }

    public void setMiddleHand(List<Card> middleHand) {
        this.middleHand = middleHand;
    }

    public List<Card> getBackHand() {
        return backHand;
    }

    public void setBackHand(List<Card> backHand) {
        this.backHand = backHand;
    }
}
