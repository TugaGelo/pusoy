package com.pusoygame.pusoybackend;

import java.util.*;
import java.util.stream.Collectors;

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

    private static int compareFiveCardHands(Hand hand1, Hand hand2) {
        HandRank rank1 = evaluateFiveCardHand(hand1);
        HandRank rank2 = evaluateFiveCardHand(hand2);

        int rankComparison = rank1.compareTo(rank2);
        if (rankComparison != 0) {
            return rankComparison;
        }

        return compareFiveCardHandsWithSameRank(hand1, hand2);
    }

    private static int compareThreeCardHands(Hand hand1, Hand hand2) {
        HandRank rank1 = evaluateThreeCardHand(hand1);
        HandRank rank2 = evaluateThreeCardHand(hand2);

        int rankComparison = rank1.compareTo(rank2);
        if (rankComparison != 0) {
            return rankComparison;
        }

        return compareThreeCardHandsWithSameRank(hand1, hand2);
    }

    public static HandRank evaluateFiveCardHand(Hand hand) {
        hand.sortCardsByRank();
        Map<Integer, Long> rankCounts = hand.getRankCounts();

        boolean isFlush = isFlush(hand);
        boolean isStraight = isStraight(hand);

        if (isStraight && isFlush && hand.getCards().get(4).getRank() == 14) {
            return HandRank.ROYAL_FLUSH;
        }
        if (isStraight && isFlush) return HandRank.STRAIGHT_FLUSH;
        if (rankCounts.containsValue(4L)) return HandRank.FOUR_OF_A_KIND;
        if (rankCounts.containsValue(3L) && rankCounts.containsValue(2L)) return HandRank.FULL_HOUSE;
        if (isFlush) return HandRank.FLUSH;
        if (isStraight) return HandRank.STRAIGHT;
        if (rankCounts.containsValue(3L)) return HandRank.THREE_OF_A_KIND;
        if (rankCounts.values().stream().filter(c -> c == 2L).count() == 2) return HandRank.TWO_PAIR;
        if (rankCounts.containsValue(2L)) return HandRank.PAIR;
        return HandRank.HIGH_CARD;
    }

    public static HandRank evaluateThreeCardHand(Hand hand) {
        Map<Integer, Long> rankCounts = hand.getRankCounts();

        if (rankCounts.containsValue(3L)) return HandRank.THREE_OF_A_KIND;
        if (rankCounts.containsValue(2L)) return HandRank.PAIR;
        return HandRank.HIGH_CARD;
    }

    private static int compareFiveCardHandsWithSameRank(Hand hand1, Hand hand2) {
        HandRank rank = evaluateFiveCardHand(hand1);
        Map<Integer, Long> counts1 = hand1.getRankCounts();
        Map<Integer, Long> counts2 = hand2.getRankCounts();

        switch (rank) {
            case PAIR:
            case TWO_PAIR:
            case THREE_OF_A_KIND:
            case FULL_HOUSE:
            case FOUR_OF_A_KIND:
                List<Integer> rankedRanks1 = getRankedRanks(counts1);
                List<Integer> rankedRanks2 = getRankedRanks(counts2);
                for (int i = 0; i < rankedRanks1.size(); i++) {
                    int cmp = Integer.compare(rankedRanks1.get(i), rankedRanks2.get(i));
                    if (cmp != 0) return cmp;
                }
                break;

            case STRAIGHT:
            case STRAIGHT_FLUSH:
                return Integer.compare(getStraightTopCard(hand1), getStraightTopCard(hand2));

            default:
                List<Card> cards1 = hand1.getCards();
                List<Card> cards2 = hand2.getCards();
                for (int i = cards1.size() - 1; i >= 0; i--) {
                    int cmp = Integer.compare(cards1.get(i).getRank(), cards2.get(i).getRank());
                    if (cmp != 0) return cmp;
                }
        }
        return 0;
    }

    private static int compareThreeCardHandsWithSameRank(Hand hand1, Hand hand2) {
        HandRank rank = evaluateThreeCardHand(hand1);
        Map<Integer, Long> counts1 = hand1.getRankCounts();
        Map<Integer, Long> counts2 = hand2.getRankCounts();

        switch (rank) {
            case PAIR:
            case THREE_OF_A_KIND:
                List<Integer> rankedRanks1 = getRankedRanks(counts1);
                List<Integer> rankedRanks2 = getRankedRanks(counts2);
                for (int i = 0; i < rankedRanks1.size(); i++) {
                    int cmp = Integer.compare(rankedRanks1.get(i), rankedRanks2.get(i));
                    if (cmp != 0) return cmp;
                }
                break;

            default:
                List<Card> cards1 = hand1.getCards();
                List<Card> cards2 = hand2.getCards();
                for (int i = cards1.size() - 1; i >= 0; i--) {
                    int cmp = Integer.compare(cards1.get(i).getRank(), cards2.get(i).getRank());
                    if (cmp != 0) return cmp;
                }
        }
        return 0;
    }

    private static List<Integer> getRankedRanks(Map<Integer, Long> counts) {
        List<Integer> result = new ArrayList<>();
        counts.entrySet().stream().filter(e -> e.getValue() == 4).map(Map.Entry::getKey).sorted(Comparator.reverseOrder()).forEach(result::add);
        counts.entrySet().stream().filter(e -> e.getValue() == 3).map(Map.Entry::getKey).sorted(Comparator.reverseOrder()).forEach(result::add);
        counts.entrySet().stream().filter(e -> e.getValue() == 2).map(Map.Entry::getKey).sorted(Comparator.reverseOrder()).forEach(result::add);
        counts.entrySet().stream().filter(e -> e.getValue() == 1).map(Map.Entry::getKey).sorted(Comparator.reverseOrder()).forEach(result::add);
        return result;
    }

    private static boolean isStraight(Hand hand) {
        // Get sorted list of unique ranks
        List<Integer> ranks = hand.getCards().stream()
                .map(Card::getRank)
                .distinct() // <-- remove duplicates
                .sorted()
                .collect(Collectors.toList());

        // Special case: A2345 (Ace can be low)
        if (ranks.contains(14) && ranks.contains(2) && ranks.contains(3) && ranks.contains(4) && ranks.contains(5)) {
            return true;
        }

        // Now check for 5 consecutive numbers
        for (int i = 0; i <= ranks.size() - 5; i++) {
            boolean straight = true;
            for (int j = 0; j < 4; j++) {
                if (ranks.get(i + j) + 1 != ranks.get(i + j + 1)) {
                    straight = false;
                    break;
                }
            }
            if (straight) return true;
        }

        return false;
    }

    private static int getStraightTopCard(Hand hand) {
        List<Integer> ranks = hand.getCards().stream()
                .map(Card::getRank)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        if (ranks.equals(List.of(2, 3, 4, 5, 14))) return 5; // wheel straight
        return ranks.get(ranks.size() - 1);
    }

    private static boolean isFlush(Hand hand) {
        Set<String> suits = hand.getCards().stream()
                .map(Card::getSuit)
                .collect(Collectors.toSet());
        return suits.size() == 1;
    }

    // ✅ Public wrapper for getting hand name
    public static String getHandName(Hand hand) {
        HandRank rank;
        if (hand.getCards().size() == 5) {
            rank = evaluateFiveCardHand(hand);
        } else if (hand.getCards().size() == 3) {
            rank = evaluateThreeCardHand(hand);
        } else {
            rank = HandRank.HIGH_CARD;
        }
        return rank.toString().replace("_", " ");
    }
}
