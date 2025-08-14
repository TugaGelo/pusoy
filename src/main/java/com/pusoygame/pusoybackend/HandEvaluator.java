package com.pusoygame.pusoybackend;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// The HandEvaluator class is responsible for determining the rank of a given poker hand.
// This is a crucial component for both game validation and AI logic.
public class HandEvaluator {

    // An enum to represent the possible poker hand ranks.
    public enum HandRank {
        HIGH_CARD,
        PAIR,
        TWO_PAIR,
        THREE_OF_A_KIND,
        STRAIGHT,
        FLUSH,
        FULL_HOUSE,
        FOUR_OF_A_KIND,
        STRAIGHT_FLUSH,
        ROYAL_FLUSH
    }

    // This method will be used to determine the rank of a 5-card hand.
    public static HandRank evaluateFiveCardHand(Hand hand) {
        hand.sortCardsByRank();
        Map<Integer, Long> rankCounts = hand.getRankCounts();

        boolean isFlush = isFlush(hand);
        boolean isStraight = isStraight(hand);

        // Check for Four of a Kind
        if (rankCounts.containsValue(4L)) {
            return HandRank.FOUR_OF_A_KIND;
        }
        
        // Check for a Full House
        if (rankCounts.containsValue(3L) && rankCounts.containsValue(2L)) {
            return HandRank.FULL_HOUSE;
        }

        // Check for a Flush
        if (isFlush) {
            return HandRank.FLUSH;
        }

        // Check for a Straight
        if (isStraight) {
            return HandRank.STRAIGHT;
        }

        // Check for Three of a Kind
        if (rankCounts.containsValue(3L)) {
            return HandRank.THREE_OF_A_KIND;
        }

        // Check for Two Pair
        if (rankCounts.values().stream().filter(count -> count == 2L).count() == 2) {
            return HandRank.TWO_PAIR;
        }

        // Check for a Pair
        if (rankCounts.containsValue(2L)) {
            return HandRank.PAIR;
        }

        return HandRank.HIGH_CARD;
    }

    // This method will be used to determine the rank of a 3-card hand.
    public static HandRank evaluateThreeCardHand(Hand hand) {
        Map<Integer, Long> rankCounts = hand.getRankCounts();
        
        // Check for Three of a Kind
        if (rankCounts.containsValue(3L)) {
            return HandRank.THREE_OF_A_KIND;
        }

        // Check for a Pair
        if (rankCounts.containsValue(2L)) {
            return HandRank.PAIR;
        }
        
        return HandRank.HIGH_CARD;
    }

    // --- New helper methods for hand evaluation ---

    // This method checks if a 5-card hand is a straight.
    private static boolean isStraight(Hand hand) {
        // A straight is a hand with five cards of sequential rank.
        // The hand must already be sorted for this to work correctly.
        List<Card> sortedCards = hand.getCards();
        for (int i = 0; i < sortedCards.size() - 1; i++) {
            if (sortedCards.get(i).getRank() + 1 != sortedCards.get(i + 1).getRank()) {
                return false;
            }
        }
        return true;
    }

    // This method checks if a 5-card hand is a flush.
    private static boolean isFlush(Hand hand) {
        // A flush is a hand where all cards have the same suit.
        Set<String> suits = hand.getCards().stream()
                .map(Card::getSuit)
                .collect(Collectors.toSet());
        return suits.size() == 1;
    }
}
