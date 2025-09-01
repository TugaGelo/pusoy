package com.pusoygame.pusoybackend;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Detects the special auto-win hands you specified:
 *
 * Ranking (weakest -> strongest):
 *  - SIX_PAIRS
 *  - THREE_STRAIGHTS
 *  - THREE_FLUSHES
 *  - THREE_STRAIGHT_FLUSHES
 *  - DRAGON_STRAIGHT  (A..K present)
 *  - DRAGON_STRAIGHT_FLUSH (A..K present and all same suit)
 *
 * Usage: call detectAutoWin(cards13) with a list of 13 Card objects.
 */
public class AutoWinChecker {

    public enum AutoWinType {
        NONE,
        SIX_PAIRS,
        THREE_STRAIGHTS,
        THREE_FLUSHES,
        THREE_STRAIGHT_FLUSHES,
        DRAGON_STRAIGHT,
        DRAGON_STRAIGHT_FLUSH
    }

    /**
     * Detect the strongest applicable auto-win for the given 13 cards.
     */
    public static AutoWinType detectAutoWin(List<Card> cards13) {
        if (cards13 == null || cards13.size() != 13) return AutoWinType.NONE;

        // Strongest first
        if (isDragonStraightFlush(cards13)) return AutoWinType.DRAGON_STRAIGHT_FLUSH;
        if (isDragonStraight(cards13)) return AutoWinType.DRAGON_STRAIGHT;
        if (hasThreeStraightFlushes(cards13)) return AutoWinType.THREE_STRAIGHT_FLUSHES;
        if (hasThreeFlushes(cards13)) return AutoWinType.THREE_FLUSHES;
        if (hasThreeStraights(cards13)) return AutoWinType.THREE_STRAIGHTS;
        if (hasSixPairs(cards13)) return AutoWinType.SIX_PAIRS;

        return AutoWinType.NONE;
    }

    // ---------- checks ----------

    // 6 pairs anywhere among 13 cards
    public static boolean hasSixPairs(List<Card> cards13) {
        int[] counts = new int[15];
        for (Card c : cards13) counts[c.getRank()]++;
        int pairs = 0;
        for (int r = 2; r <= 14; r++) {
            pairs += counts[r] / 2;
        }
        return pairs >= 6;
    }

    // Dragon straight: contains all ranks 2..14 (Ace=14)
    public static boolean isDragonStraight(List<Card> cards13) {
        boolean[] present = new boolean[15];
        for (Card c : cards13) present[c.getRank()] = true;
        for (int r = 2; r <= 14; r++) {
            if (!present[r]) return false;
        }
        return true;
    }

    // Dragon straight flush: dragon AND all same suit
    public static boolean isDragonStraightFlush(List<Card> cards13) {
        if (!isDragonStraight(cards13)) return false;
        String suit = cards13.get(0).getSuit();
        for (Card c : cards13) {
            if (!c.getSuit().equals(suit)) return false;
        }
        return true;
    }

    // 3 flushes: there exists a split where both 5-card hands are flushes (or straight-flush/royal-flush) AND the remaining 3 are same suit
    public static boolean hasThreeFlushes(List<Card> cards13) {
        // iterate all 5-card combos for back, then all 5-card combos for middle from remaining
        List<List<Card>> first5 = combinations5(cards13);
        for (List<Card> a : first5) {
            List<Card> rem8 = subtract(cards13, a);
            List<List<Card>> second5 = combinations5(rem8);
            for (List<Card> b : second5) {
                List<Card> back = a;
                List<Card> middle = b;
                // back and middle must be "flush-like" (FLUSH or STRAIGHT_FLUSH or ROYAL_FLUSH)
                if (!isFlushLike(back) || !isFlushLike(middle)) continue;
                // front = remaining 3
                List<Card> front = subtract(rem8, b);
                if (front.size() != 3) continue;
                if (isThreeCardFlush(front)) return true;
            }
        }
        return false;
    }

    // 3 straights: both back and middle are straight-like AND front 3 are straight (3-card consecutive)
    public static boolean hasThreeStraights(List<Card> cards13) {
        List<List<Card>> first5 = combinations5(cards13);
        for (List<Card> a : first5) {
            List<Card> rem8 = subtract(cards13, a);
            List<List<Card>> second5 = combinations5(rem8);
            for (List<Card> b : second5) {
                List<Card> back = a;
                List<Card> middle = b;
                if (!isStraightLike(back) || !isStraightLike(middle)) continue;
                List<Card> front = subtract(rem8, b);
                if (front.size() != 3) continue;
                if (isThreeCardStraight(front)) return true;
            }
        }
        return false;
    }

    // 3 straight flushes: both back/middle are straight-flush-like and front is 3-card straight-flush
    public static boolean hasThreeStraightFlushes(List<Card> cards13) {
        List<List<Card>> first5 = combinations5(cards13);
        for (List<Card> a : first5) {
            List<Card> rem8 = subtract(cards13, a);
            List<List<Card>> second5 = combinations5(rem8);
            for (List<Card> b : second5) {
                List<Card> back = a;
                List<Card> middle = b;
                if (!isStraightFlushLike(back) || !isStraightFlushLike(middle)) continue;
                List<Card> front = subtract(rem8, b);
                if (front.size() != 3) continue;
                if (isThreeCardStraightFlush(front)) return true;
            }
        }
        return false;
    }

    // ---------- small helpers ----------

    // treat STRAIGHT_FLUSH and ROYAL_FLUSH as straight-flush-like
    private static boolean isStraightFlushLike(List<Card> five) {
        Hand h = new Hand(new ArrayList<>(five));
        HandEvaluator.HandRank r = HandEvaluator.evaluateFiveCardHand(h);
        return r == HandEvaluator.HandRank.STRAIGHT_FLUSH || r == HandEvaluator.HandRank.ROYAL_FLUSH;
    }

    // treat STRAIGHT and STRAIGHT_FLUSH and ROYAL_FLUSH as straight-like
    private static boolean isStraightLike(List<Card> five) {
        Hand h = new Hand(new ArrayList<>(five));
        HandEvaluator.HandRank r = HandEvaluator.evaluateFiveCardHand(h);
        return r == HandEvaluator.HandRank.STRAIGHT
                || r == HandEvaluator.HandRank.STRAIGHT_FLUSH
                || r == HandEvaluator.HandRank.ROYAL_FLUSH;
    }

    // treat FLUSH and STRAIGHT_FLUSH and ROYAL_FLUSH as flush-like
    private static boolean isFlushLike(List<Card> five) {
        Hand h = new Hand(new ArrayList<>(five));
        HandEvaluator.HandRank r = HandEvaluator.evaluateFiveCardHand(h);
        return r == HandEvaluator.HandRank.FLUSH
                || r == HandEvaluator.HandRank.STRAIGHT_FLUSH
                || r == HandEvaluator.HandRank.ROYAL_FLUSH;
    }

    // 3-card flush
    private static boolean isThreeCardFlush(List<Card> three) {
        if (three.size() != 3) return false;
        String suit = three.get(0).getSuit();
        for (Card c : three) if (!c.getSuit().equals(suit)) return false;
        return true;
    }

    // 3-card straight (consecutive ranks). Support A-2-3 as wheel.
    private static boolean isThreeCardStraight(List<Card> three) {
        if (three.size() != 3) return false;
        List<Integer> ranks = new ArrayList<>();
        for (Card c : three) ranks.add(c.getRank());
        ranks.sort(Comparator.naturalOrder());
        // remove duplicates (shouldn't have dups for three-card straight test to succeed)
        Set<Integer> set = new HashSet<>(ranks);
        if (set.size() != 3) return false;
        // A-2-3 special (14,2,3)
        if (set.contains(14) && set.contains(2) && set.contains(3)) return true;
        Integer[] arr = set.toArray(new Integer[0]);
        java.util.Arrays.sort(arr);
        return arr[1] == arr[0] + 1 && arr[2] == arr[1] + 1;
    }

    // 3-card straight-flush: same suit + straight
    private static boolean isThreeCardStraightFlush(List<Card> three) {
        return isThreeCardFlush(three) && isThreeCardStraight(three);
    }

    // ---------- utility combination helpers (same identity semantics as original Game) ----------

    // generate all 5-card combos (references) from list
    private static List<List<Card>> combinations5(List<Card> cards) {
        List<List<Card>> result = new ArrayList<>();
        int n = cards.size();
        if (n < 5) return result;
        for (int a = 0; a <= n - 5; a++) {
            for (int b = a + 1; b <= n - 4; b++) {
                for (int c = b + 1; c <= n - 3; c++) {
                    for (int d = c + 1; d <= n - 2; d++) {
                        for (int e = d + 1; e <= n - 1; e++) {
                            List<Card> combo = new ArrayList<>(5);
                            combo.add(cards.get(a));
                            combo.add(cards.get(b));
                            combo.add(cards.get(c));
                            combo.add(cards.get(d));
                            combo.add(cards.get(e));
                            result.add(combo);
                        }
                    }
                }
            }
        }
        return result;
    }

    // subtract by identity
    private static List<Card> subtract(List<Card> from, List<Card> toRemove) {
        List<Card> result = new ArrayList<>(from);
        for (Card r : toRemove) {
            for (int i = 0; i < result.size(); i++) {
                if (result.get(i) == r) {
                    result.remove(i);
                    break;
                }
            }
        }
        return result;
    }
}
