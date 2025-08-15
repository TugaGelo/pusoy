package com.pusoygame.pusoybackend;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// The HandEvaluator class is responsible for determining the rank of a given poker hand.
public class HandEvaluator {

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
    
    // This method compares two hands and returns which one is stronger.
    public static int compareHands(Hand hand1, Hand hand2) {
        if (hand1.getCards().size() > hand2.getCards().size()) {
            return 1;
        } else if (hand1.getCards().size() < hand2.getCards().size()) {
            return -1;
        }

        if (hand1.getCards().size() == 5) {
            return compareFiveCardHands(hand1, hand2);
        } else if (hand1.getCards().size() == 3) {
            return compareThreeCardHands(hand1, hand2);
        }
        
        return 0;
    }

    // A method for comparing two 5-card hands.
    private static int compareFiveCardHands(Hand hand1, Hand hand2) {
        HandRank rank1 = evaluateFiveCardHand(hand1);
        HandRank rank2 = evaluateFiveCardHand(hand2);

        int rankComparison = rank1.compareTo(rank2);
        if (rankComparison != 0) {
            return rankComparison;
        }

        return compareHandsWithSameRank(hand1, hand2);
    }

    // A method for comparing two 3-card hands.
    private static int compareThreeCardHands(Hand hand1, Hand hand2) {
        HandRank rank1 = evaluateThreeCardHand(hand1);
        HandRank rank2 = evaluateThreeCardHand(hand2);

        int rankComparison = rank1.compareTo(rank2);
        if (rankComparison != 0) {
            return rankComparison;
        }

        return compareHandsWithSameRank(hand1, hand2);
    }

    // A method to determine the rank of a 5-card hand.
    public static HandRank evaluateFiveCardHand(Hand hand) {
        hand.sortCardsByRank();
        Map<Integer, Long> rankCounts = hand.getRankCounts();

        boolean isFlush = isFlush(hand);
        boolean isStraight = isStraight(hand);

        if (isStraight && isFlush && hand.getCards().get(4).getRank() == 14) {
            return HandRank.ROYAL_FLUSH;
        }

        if (isStraight && isFlush) {
            return HandRank.STRAIGHT_FLUSH;
        }

        if (rankCounts.containsValue(4L)) {
            return HandRank.FOUR_OF_A_KIND;
        }
        
        if (rankCounts.containsValue(3L) && rankCounts.containsValue(2L)) {
            return HandRank.FULL_HOUSE;
        }

        if (isFlush) {
            return HandRank.FLUSH;
        }

        if (isStraight) {
            return HandRank.STRAIGHT;
        }

        if (rankCounts.containsValue(3L)) {
            return HandRank.THREE_OF_A_KIND;
        }

        if (rankCounts.values().stream().filter(count -> count == 2L).count() == 2) {
            return HandRank.TWO_PAIR;
        }

        if (rankCounts.containsValue(2L)) {
            return HandRank.PAIR;
        }

        return HandRank.HIGH_CARD;
    }

    // A method to determine the rank of a 3-card hand.
    public static HandRank evaluateThreeCardHand(Hand hand) {
        Map<Integer, Long> rankCounts = hand.getRankCounts();
        
        if (rankCounts.containsValue(3L)) {
            return HandRank.THREE_OF_A_KIND;
        }

        if (rankCounts.containsValue(2L)) {
            return HandRank.PAIR;
        }
        
        return HandRank.HIGH_CARD;
    }

    // A method that compares two hands of the same rank to break a tie.
    private static int compareHandsWithSameRank(Hand hand1, Hand hand2) {
        HandRank rank = evaluateFiveCardHand(hand1);

        Map<Integer, Long> counts1 = hand1.getRankCounts();
        Map<Integer, Long> counts2 = hand2.getRankCounts();

        if (rank == HandRank.FULL_HOUSE) {
            int triple1 = counts1.entrySet().stream()
                .filter(e -> e.getValue() == 3)
                .map(Map.Entry::getKey)
                .findFirst().orElse(0);
            int triple2 = counts2.entrySet().stream()
                .filter(e -> e.getValue() == 3)
                .map(Map.Entry::getKey)
                .findFirst().orElse(0);

            if (triple1 != triple2) {
                return Integer.compare(triple1, triple2);
            }

            int pair1 = counts1.entrySet().stream()
                .filter(e -> e.getValue() == 2)
                .map(Map.Entry::getKey)
                .findFirst().orElse(0);
            int pair2 = counts2.entrySet().stream()
                .filter(e -> e.getValue() == 2)
                .map(Map.Entry::getKey)
                .findFirst().orElse(0);

            return Integer.compare(pair1, pair2);
        }

        // fallback for other hand types
        List<Card> cards1 = hand1.getCards();
        List<Card> cards2 = hand2.getCards();
        for (int i = cards1.size() - 1; i >= 0; i--) {
            int cmp = Integer.compare(cards1.get(i).getRank(), cards2.get(i).getRank());
            if (cmp != 0) return cmp;
        }
        return 0;
    }

    // A method that checks if a 5-card hand is a straight.
    private static boolean isStraight(Hand hand) {
        List<Integer> ranks = hand.getCards().stream()
            .map(Card::getRank)
            .distinct()
            .sorted()
            .collect(Collectors.toList());

        // Special case: Ace-low straight (A=14, but treat as 1)
        if (ranks.equals(List.of(2, 3, 4, 5, 14))) {
            return true;
        }

        for (int i = 0; i < ranks.size() - 1; i++) {
            if (ranks.get(i) + 1 != ranks.get(i + 1)) {
                return false;
            }
        }
        return true;
    }


    // A method that checks if a 5-card hand is a flush.
    private static boolean isFlush(Hand hand) {
        Set<String> suits = hand.getCards().stream()
                .map(Card::getSuit)
                .collect(Collectors.toSet());
        return suits.size() == 1;
    }
}
