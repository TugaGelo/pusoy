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
    
    // This new method compares two hands and returns which one is stronger.
    // It returns a positive number if hand1 is stronger, a negative number if hand2 is stronger,
    // and 0 if the hands are of equal strength.
    public static int compareHands(Hand hand1, Hand hand2) {
        // --- FIX: Handle hands of different sizes first ---
        if (hand1.getCards().size() > hand2.getCards().size()) {
            return 1;
        } else if (hand1.getCards().size() < hand2.getCards().size()) {
            return -1;
        }
        // --- END FIX ---

        // If the hands are of the same size, we proceed with the existing logic.
        if (hand1.getCards().size() == 5) {
            return compareFiveCardHands(hand1, hand2);
        } else if (hand1.getCards().size() == 3) {
            return compareThreeCardHands(hand1, hand2);
        }
        
        return 0;
    }

    // A dedicated method for comparing two 5-card hands.
    private static int compareFiveCardHands(Hand hand1, Hand hand2) {
        HandRank rank1 = evaluateFiveCardHand(hand1);
        HandRank rank2 = evaluateFiveCardHand(hand2);

        int rankComparison = rank1.compareTo(rank2);
        if (rankComparison != 0) {
            return rankComparison;
        }

        return compareHandsWithSameRank(hand1, hand2);
    }

    // A dedicated method for comparing two 3-card hands.
    private static int compareThreeCardHands(Hand hand1, Hand hand2) {
        HandRank rank1 = evaluateThreeCardHand(hand1);
        HandRank rank2 = evaluateThreeCardHand(hand2);

        int rankComparison = rank1.compareTo(rank2);
        if (rankComparison != 0) {
            return rankComparison;
        }

        return compareHandsWithSameRank(hand1, hand2);
    }

    // This method will be used to determine the rank of a 5-card hand.
    public static HandRank evaluateFiveCardHand(Hand hand) {
        hand.sortCardsByRank();
        Map<Integer, Long> rankCounts = hand.getRankCounts();

        boolean isFlush = isFlush(hand);
        boolean isStraight = isStraight(hand);

        // Check for a Royal Flush
        // This is the strongest hand, so we check for it first.
        if (isStraight && isFlush && hand.getCards().get(4).getRank() == 14) {
            return HandRank.ROYAL_FLUSH;
        }

        // Check for a Straight Flush
        // This is the second strongest hand.
        if (isStraight && isFlush) {
            return HandRank.STRAIGHT_FLUSH;
        }

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

    // --- Helper methods for hand evaluation ---

    // This new method compares two hands of the same rank to break a tie.
    private static int compareHandsWithSameRank(Hand hand1, Hand hand2) {
        // The hands are already sorted.
        List<Card> cards1 = hand1.getCards();
        List<Card> cards2 = hand2.getCards();
        
        // The hands should have the same size.
        for (int i = cards1.size() - 1; i >= 0; i--) {
            int cardComparison = Integer.compare(cards1.get(i).getRank(), cards2.get(i).getRank());
            if (cardComparison != 0) {
                return cardComparison;
            }
        }

        // If all cards have the same rank, the hands are a tie.
        return 0;
    }

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
