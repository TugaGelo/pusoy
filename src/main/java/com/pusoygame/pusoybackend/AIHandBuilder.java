package com.pusoygame.pusoybackend;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * AIHandBuilder contains the logic to split a 13-card hand into back(5), middle(5), front(3).
 * This is a refactor of the AI logic previously embedded in Game.java.
 *
 * Public API: buildBestPartition(cards13) -> returns Partition or null if none found.
 */
public class AIHandBuilder {

    /**
     * Build best partition using heuristics and AI preferences.
     * Returns null if no candidate partition is found.
     */
    public static Partition buildBestPartition(List<Card> cards13) {
        if (cards13 == null || cards13.size() != 13) return null;

        // Work on a stable sorted copy (ascending by rank) to get deterministic results
        List<Card> pool = new ArrayList<>(cards13);
        pool.sort(Comparator.comparingInt(card -> card.getRank().getValue()));

        List<Partition> candidates = new ArrayList<>();

        List<List<Card>> firstFives = combinations5(pool);
        for (List<Card> fiveA : firstFives) {
            List<Card> rem8 = subtract(pool, fiveA);
            List<List<Card>> secondFives = combinations5(rem8);
            for (List<Card> fiveB : secondFives) {
                Hand hA = new Hand(new ArrayList<>(fiveA));
                Hand hB = new Hand(new ArrayList<>(fiveB));
                hA.sortCardsByRank();
                hB.sortCardsByRank();
                int cmp = HandEvaluator.compareHands(hA, hB);
                if (cmp == 0) continue; // back must be strictly > middle
                List<Card> back = (cmp > 0) ? fiveA : fiveB;
                List<Card> middle = (cmp > 0) ? fiveB : fiveA;

                List<Card> pool8 = subtract(pool, back);
                Partition normalized = normalizeMiddleAndFront(back, middle, pool8);
                // Ensure strict back > middle after normalization
                if (HandEvaluator.compareHands(new Hand(normalized.back), new Hand(normalized.middle)) <= 0) {
                    continue;
                }
                candidates.add(normalized);
            }
        }

        if (candidates.isEmpty()) return null;

        // sort candidates by AI preference
        candidates.sort((p1, p2) -> {
            int backCmp = aiCompareHandsForSorting(new Hand(p1.back), new Hand(p2.back));
            if (backCmp != 0) return -backCmp;
            int midCmp = aiCompareHandsForSorting(new Hand(p1.middle), new Hand(p2.middle));
            if (midCmp != 0) return -midCmp;
            int frontCmp = HandEvaluator.compareHands(new Hand(p1.front), new Hand(p2.front));
            return -frontCmp;
        });

        return candidates.get(0);
    }

    // ---------- normalization logic with high-card fallback ----------
    private static Partition normalizeMiddleAndFront(List<Card> back, List<Card> proposedMiddle, List<Card> pool8) {
        Hand midH = new Hand(new ArrayList<>(proposedMiddle));
        HandEvaluator.HandRank mRank = HandEvaluator.evaluateFiveCardHand(midH);

        // Check if all remaining hands can only be high cards
        boolean allHighCards = true;
        HandEvaluator.HandRank backRank = HandEvaluator.evaluateFiveCardHand(new Hand(back));
        HandEvaluator.HandRank proposedMiddleRank = HandEvaluator.evaluateFiveCardHand(new Hand(proposedMiddle));
        HandEvaluator.HandRank remainingRank = HandEvaluator.evaluateFiveCardHand(new Hand(pool8));
        if (mRank != HandEvaluator.HandRank.HIGH_CARD) allHighCards = false;
        for (Card c : pool8) {
            if (c == null) continue;
        }

        if (allHighCards) {
            // Sort pool8 ascending by rank
            List<Card> sorted = new ArrayList<>(pool8);
            sorted.sort(Comparator.comparingInt(card -> card.getRank().getValue()));

            // Middle: 4 lowest + highest
            List<Card> middle = new ArrayList<>();
            middle.add(sorted.get(sorted.size() - 1)); // highest
            middle.addAll(sorted.subList(0, Math.min(4, sorted.size() - 1))); // 4 lowest

            // Front: remaining 3
            List<Card> front = new ArrayList<>();
            for (Card c : sorted) {
                if (!middle.contains(c)) front.add(c);
            }

            return new Partition(new ArrayList<>(back), middle, front);
        }

        // special handling for pair/two_pair/trips to reserve top cards for front
        if (mRank == HandEvaluator.HandRank.PAIR
                || mRank == HandEvaluator.HandRank.TWO_PAIR
                || mRank == HandEvaluator.HandRank.THREE_OF_A_KIND) {

            List<Card> core = pickLowestCoreFor(pool8, mRank);
            if (core == null || core.isEmpty()) {
                List<Card> front = subtract(pool8, proposedMiddle);
                return new Partition(new ArrayList<>(back), new ArrayList<>(proposedMiddle), front);
            }
            List<Card> remaining = subtract(pool8, core);
            remaining.sort(Comparator.comparingInt(card -> card.getRank().getValue()));
            int need = 5 - core.size();
            List<Card> fillers = new ArrayList<>();
            for (int i = 0; i < need && i < remaining.size(); i++) fillers.add(remaining.get(i));
            List<Card> newMiddle = new ArrayList<>(core);
            newMiddle.addAll(fillers);
            List<Card> afterMiddle = subtract(pool8, newMiddle);
            afterMiddle.sort(Comparator.comparingInt(card -> card.getRank().getValue()));
            List<Card> newFront = new ArrayList<>();
            int n = afterMiddle.size();
            if (n >= 3) {
                newFront.add(afterMiddle.get(n - 1));
                newFront.add(afterMiddle.get(n - 2));
                newFront.add(afterMiddle.get(n - 3));
            } else {
                newFront.addAll(afterMiddle);
            }
            return new Partition(new ArrayList<>(back), newMiddle, newFront);
        }

        // keep original for other ranks
        List<Card> front = subtract(pool8, proposedMiddle);
        return new Partition(new ArrayList<>(back), new ArrayList<>(proposedMiddle), front);
    }

    private static List<Card> pickLowestCoreFor(List<Card> pool8, HandEvaluator.HandRank category) {
        int[] counts = new int[15];
        for (Card c : pool8) counts[c.getRank().getValue()]++;
        if (category == HandEvaluator.HandRank.THREE_OF_A_KIND) {
            for (int r = 2; r <= 14; r++) if (counts[r] >= 3) return takeNOfRank(pool8, r, 3);
            return null;
        } else if (category == HandEvaluator.HandRank.TWO_PAIR) {
            Integer first = null, second = null;
            for (int r = 2; r <= 14; r++) {
                if (counts[r] >= 2) {
                    if (first == null) first = r;
                    else {
                        second = r;
                        break;
                    }
                }
            }
            if (first != null && second != null) {
                List<Card> core = new ArrayList<>();
                core.addAll(takeNOfRank(pool8, first, 2));
                core.addAll(takeNOfRank(pool8, second, 2));
                return core;
            }
            return null;
        } else if (category == HandEvaluator.HandRank.PAIR) {
            for (int r = 2; r <= 14; r++) if (counts[r] >= 2) return takeNOfRank(pool8, r, 2);
            return null;
        }
        return null;
    }

    private static List<Card> takeNOfRank(List<Card> cards, int rank, int n) {
        List<Card> out = new ArrayList<>();
        for (Card c : cards) {
            if (c.getRank().getValue() == rank) {
                out.add(c);
                if (out.size() == n) break;
            }
        }
        return out;
    }

    // ---------- AI comparator and helpers (preserve prior preferences) ----------
    private static int aiCompareHandsForSorting(Hand h1, Hand h2) {
        if (h1.getCards().size() == 5 && h2.getCards().size() == 5) {
            HandEvaluator.HandRank r1 = HandEvaluator.evaluateFiveCardHand(h1);
            HandEvaluator.HandRank r2 = HandEvaluator.evaluateFiveCardHand(h2);

            if (r1 == HandEvaluator.HandRank.FOUR_OF_A_KIND && r2 == HandEvaluator.HandRank.FOUR_OF_A_KIND) {
                int q1 = getQuadsRankIfFourKind(h1);
                int q2 = getQuadsRankIfFourKind(h2);
                if (q1 != q2) return Integer.compare(q1, q2); // higher quads preferred
                int k1 = getKickerRankIfFourKind(h1);
                int k2 = getKickerRankIfFourKind(h2);
                return Integer.compare(k2, k1); // prefer lower kicker
            }

            if (r1 == HandEvaluator.HandRank.FULL_HOUSE && r2 == HandEvaluator.HandRank.FULL_HOUSE) {
                int t1 = getTripsRankIfFullHouse(h1);
                int t2 = getTripsRankIfFullHouse(h2);
                if (t1 != t2) return Integer.compare(t1, t2);
                int p1 = getPairRankIfFullHouse(h1, true);
                int p2 = getPairRankIfFullHouse(h2, true);
                return Integer.compare(p2, p1); // prefer lower pair when trips equal
            }
        }
        return HandEvaluator.compareHands(h1, h2);
    }

    private static int getTripsRankIfFullHouse(Hand h) {
        return h.getRankCounts().entrySet().stream()
                .filter(e -> e.getValue() == 3L).mapToInt(e -> e.getKey()).findFirst().orElse(-1);
    }

    private static int getPairRankIfFullHouse(Hand h, boolean lowest) {
        return h.getRankCounts().entrySet().stream()
                .filter(e -> e.getValue() == 2L)
                .mapToInt(e -> e.getKey())
                .boxed()
                .sorted(lowest ? Integer::compareTo : Comparator.reverseOrder())
                .findFirst()
                .orElse(-1);
    }

    private static int getQuadsRankIfFourKind(Hand h) {
        return h.getRankCounts().entrySet().stream()
                .filter(e -> e.getValue() == 4L).mapToInt(e -> e.getKey()).findFirst().orElse(-1);
    }

    private static int getKickerRankIfFourKind(Hand h) {
        return h.getRankCounts().entrySet().stream()
                .filter(e -> e.getValue() == 1L).mapToInt(e -> e.getKey()).findFirst().orElse(-1);
    }

    // ---------- minor helpers (combinations, subtract) ----------
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
