package com.pusoygame.pusoybackend;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * The Game class manages the game state, including the deck and the players.
 *
 * AI logic (setAIHands):
 *  - Enumerates all 5-card combinations from 13 cards.
 *  - Chooses two non-overlapping 5-card hands such that Back > Middle (strict).
 *  - Leftover 3 cards become Front.
 *  - Uses AI preference: when comparing FULL_HOUSE hands with the same trips,
 *    prefer the LOWER pair (e.g., AAA33 over AAAQQ).
 *  - NEW: If the chosen Middle is PAIR / TWO_PAIR / THREE_OF_A_KIND,
 *         normalize it to use the LOWEST possible core + LOWEST fillers,
 *         and reserve the TOP 3 remaining cards for the Front.
 *  - Falls back to safe split if needed.
 */
public class Game {

    private List<Card> deck;
    private List<Player> players;
    private int currentPlayerIndex;

    // Constructor to set up a new game with a list of players.
    public Game(List<Player> players) {
        this.players = players;
        this.deck = new ArrayList<>();
        initializeDeck();
        shuffleDeck();
        this.currentPlayerIndex = 0;
        dealCards();
    }

    // Creates a standard 52-card deck.
    private void initializeDeck() {
        String[] suits = {"Hearts", "Diamonds", "Clubs", "Spades"};
        for (String suit : suits) {
            for (int rank = 2; rank <= 10; rank++) {
                deck.add(new Card(suit, rank));
            }
            deck.add(new Card(suit, 11));
            deck.add(new Card(suit, 12));
            deck.add(new Card(suit, 13));
            deck.add(new Card(suit, 14));
        }
    }

    private void shuffleDeck() {
        Collections.shuffle(deck);
    }

    // Deals 13 cards to each player.
    private void dealCards() {
        for (Player player : players) {
            List<Card> playerCardList = new ArrayList<>();
            for (int i = 0; i < 13; i++) {
                playerCardList.add(deck.remove(0));
            }
            player.setHand(new Hand(playerCardList));
        }
    }

    // A method to sort the cards in a player's hand.
    public void sortPlayerHand(Player player) {
        if (player != null && player.getHand() != null) {
            Collections.sort(player.getHand().getCards(), Comparator.comparingInt(Card::getRank));
        }
    }

    // A method checks if a player's three hands are in the correct order to avoid a foul.
    public boolean checkFoul(Hand front, Hand middle, Hand back) {
        boolean backIsStrongerThanMiddle = HandEvaluator.compareHands(back, middle) > 0;
        boolean middleIsStrongerThanFront = HandEvaluator.compareHands(middle, front) > 0;
        return !(backIsStrongerThanMiddle && middleIsStrongerThanFront);
    }

    // A method to set a player's three hands and check for a foul.
    public boolean setPlayerHands(Player player, Hand front, Hand middle, Hand back) {
        if (checkFoul(front, middle, back)) {
            return false;
        }
        player.setHands(front, middle, back);
        return true;
    }

    /**
     * Build two 5-card hands (Back and Middle) from the AI's 13-card hand so that:
     *   Back > Middle (strict), and the leftover 3-card Front is maximized.
     * Uses AI preference: for FULL_HOUSE with same trips, prefer the LOWEST pair.
     * Also enforces: if Middle is pair/two pair/trips, use LOWEST core + LOWEST fillers;
     * Front keeps the TOP 3 remaining cards.
     */
    public void setAIHands(Player player) {
        // Defensive: ensure we have 13 cards.
        if (player == null || player.getHand() == null || player.getHand().getCards().size() != 13) {
            System.out.println("AI setup failed: invalid hand size.");
            return;
        }

        // Work with a stable, sorted copy (ascending rank).
        player.getHand().sortCardsByRank();
        List<Card> pool = new ArrayList<>(player.getHand().getCards());

        // Find the best partition: (Back 5) + (Middle 5) + (Front 3) with Back > Middle.
        Partition best = pickBestPartition(pool);

        if (best != null && setPlayerHands(player, new Hand(best.front), new Hand(best.middle), new Hand(best.back))) {
            System.out.println(player.getName() + " (AI) set hands: BACK=" + new Hand(best.back)
                    + ", MIDDLE=" + new Hand(best.middle) + ", FRONT=" + new Hand(best.front));
            return;
        }

        // If we couldn't find a strictly-ordered partition (rare), try tweak/swap to enforce Back > Middle.
        if (best != null) {
            boolean fixed = tryPromoteBackOverMiddle(best);
            if (fixed && setPlayerHands(player, new Hand(best.front), new Hand(best.middle), new Hand(best.back))) {
                System.out.println(player.getName() + " (AI) set hands after tweak: BACK=" + new Hand(best.back)
                        + ", MIDDLE=" + new Hand(best.middle) + ", FRONT=" + new Hand(best.front));
                return;
            }
        }

        // Final safe fallback — apply the middle normalization rule too.
        List<Card> fallback = new ArrayList<>(pool);
        Hand backHand = new Hand(new ArrayList<>(fallback.subList(8, 13)));   // strongest 5 by rank
        List<Card> pool8 = subtract(fallback, backHand.getCards());           // remaining 8

        // Start with a naive middle (next 5 by rank)
        Hand naiveMiddle = new Hand(new ArrayList<>(fallback.subList(3, 8)));
        Partition normalized = normalizeMiddleAndFront(backHand.getCards(), naiveMiddle.getCards(), pool8);

        Hand middleHand = new Hand(normalized.middle);
        Hand frontHand = new Hand(normalized.front);

        // Ensure ordering; if needed, swap back/middle (rare) and rebuild front accordingly.
        if (HandEvaluator.compareHands(backHand, middleHand) <= 0) {
            // Swap and re-normalize with new back
            List<Card> newBackCards = new ArrayList<>(middleHand.getCards());
            Hand newBack = new Hand(newBackCards);
            List<Card> pool8b = subtract(fallback, newBackCards);
            // Take lowest-core middle from pool8b
            Partition norm2 = normalizeMiddleAndFront(newBackCards, naiveMiddle.getCards(), pool8b);
            backHand = new Hand(norm2.back);
            middleHand = new Hand(norm2.middle);
            frontHand = new Hand(norm2.front);
        }

        setPlayerHands(player, frontHand, middleHand, backHand);
    }

    // A method to compare the human player's hands to each AI's hands individually.
    public void compareAllPlayerHands() {
        System.out.println("\n--- Starting the Showdown ---");
        Player humanPlayer = players.get(0);

        for (int i = 1; i < players.size(); i++) {
            Player aiPlayer = players.get(i);

            if (humanPlayer.getBackHand() != null && aiPlayer.getBackHand() != null) {
                System.out.println("Comparing hands for " + humanPlayer.getName() + " vs " + aiPlayer.getName() + ":");

                int backComparison = HandEvaluator.compareHands(humanPlayer.getBackHand(), aiPlayer.getBackHand());
                if (backComparison > 0) {
                    System.out.println("- " + humanPlayer.getName() + "'s back hand wins!");
                } else if (backComparison < 0) {
                    System.out.println("- " + aiPlayer.getName() + "'s back hand wins!");
                } else {
                    System.out.println("- Back hands are a tie!");
                }

                int middleComparison = HandEvaluator.compareHands(humanPlayer.getMiddleHand(), aiPlayer.getMiddleHand());
                if (middleComparison > 0) {
                    System.out.println("- " + humanPlayer.getName() + "'s middle hand wins!");
                } else if (middleComparison < 0) {
                    System.out.println("- " + aiPlayer.getName() + "'s middle hand wins!");
                } else {
                    System.out.println("- Middle hands are a tie!");
                }

                int frontComparison = HandEvaluator.compareHands(humanPlayer.getFrontHand(), aiPlayer.getFrontHand());
                if (frontComparison > 0) {
                    System.out.println("- " + humanPlayer.getName() + "'s front hand wins!");
                } else if (frontComparison < 0) {
                    System.out.println("- " + aiPlayer.getName() + "'s front hand wins!");
                } else {
                    System.out.println("- Front hands are a tie!");
                }
            }
        }
    }

    // --- Getter Methods ---
    public List<Player> getPlayers() {
        return players;
    }

    public List<Card> getDeck() {
        return deck;
    }

    // =========================
    // ===== AI HELPERS ========
    // =========================

    /**
     * Represents a complete split of 13 cards into Back(5), Middle(5), Front(3).
     */
    private static class Partition {
        List<Card> back;   // 5
        List<Card> middle; // 5
        List<Card> front;  // 3

        Partition(List<Card> back, List<Card> middle, List<Card> front) {
            this.back = back;
            this.middle = middle;
            this.front = front;
        }
    }

    /**
     * Pick the best partition (Back, Middle, Front) using poker evaluation + AI preference.
     * After choosing Back & Middle, if Middle is PAIR/TWO_PAIR/TRIPS, we normalize it:
     *   - Use LOWEST possible pair/two-pair/trips from the 8 cards not in Back.
     *   - Fill with LOWEST kickers.
     *   - Reserve TOP 3 remaining cards for Front.
     */
    private Partition pickBestPartition(List<Card> cards13) {
        List<Partition> candidates = new ArrayList<>();

        // Precompute all 5-card combinations from 13.
        List<List<Card>> firstFives = combinations5(cards13);

        for (List<Card> fiveA : firstFives) {
            // Remaining 8 cards after removing fiveA
            List<Card> rem8 = subtract(cards13, fiveA);

            // All 5-card combinations from the remaining 8
            List<List<Card>> secondFives = combinations5(rem8);

            for (List<Card> fiveB : secondFives) {
                // Determine which should be Back and which should be Middle (strict by standard poker)
                Hand hA = new Hand(new ArrayList<>(fiveA));
                Hand hB = new Hand(new ArrayList<>(fiveB));
                hA.sortCardsByRank();
                hB.sortCardsByRank();

                int cmp = HandEvaluator.compareHands(hA, hB);
                if (cmp == 0) {
                    // Equal strength → would foul (Back must be strictly stronger than Middle)
                    continue;
                }

                List<Card> back = (cmp > 0) ? fiveA : fiveB;
                List<Card> middle = (cmp > 0) ? fiveB : fiveA;

                // Normalize Middle if it's pair/two pair/trips, and build Front=top3 left
                Partition normalized = normalizeMiddleAndFront(back, middle, subtract(cards13, back));
                // Ensure strict Back > Middle after normalization
                if (HandEvaluator.compareHands(new Hand(normalized.back), new Hand(normalized.middle)) <= 0) {
                    continue;
                }

                candidates.add(normalized);
            }
        }

        if (candidates.isEmpty()) {
            return null;
        }

        // Sort with AI preference:
        // - Descending by AI-preferred Back
        // - Then descending by AI-preferred Middle
        // - Then descending by normal Front strength
        candidates.sort((p1, p2) -> {
            int backCmpAI = aiCompareHandsForSorting(new Hand(p1.back), new Hand(p2.back));
            if (backCmpAI != 0) return -backCmpAI; // prefer larger AI score first

            int midCmpAI = aiCompareHandsForSorting(new Hand(p1.middle), new Hand(p2.middle));
            if (midCmpAI != 0) return -midCmpAI;

            int frontCmp = HandEvaluator.compareHands(new Hand(p1.front), new Hand(p2.front));
            return -frontCmp;
        });

        // Return the top partition
        return candidates.get(0);
    }

    /**
     * Normalize Middle if it's PAIR/TWO_PAIR/THREE_OF_A_KIND:
     *  - Build LOWEST possible core (lowest pair / two lowest pairs / lowest trips) from pool8.
     *  - Fill with LOWEST remaining cards to reach 5.
     *  - Set Front to the TOP 3 remaining cards.
     * For other ranks, keep the proposed Middle and Front = (pool8 - middle).
     *
     * @param back  the chosen back 5 cards
     * @param proposedMiddle any 5-card middle proposal (used only to know the category to target)
     * @param pool8 the 8 cards not in back (i.e., cards13 - back)
     */
    private Partition normalizeMiddleAndFront(List<Card> back, List<Card> proposedMiddle, List<Card> pool8) {
        Hand midH = new Hand(new ArrayList<>(proposedMiddle));
        HandEvaluator.HandRank mRank = HandEvaluator.evaluateFiveCardHand(midH);

        if (mRank == HandEvaluator.HandRank.PAIR
                || mRank == HandEvaluator.HandRank.TWO_PAIR
                || mRank == HandEvaluator.HandRank.THREE_OF_A_KIND) {

            // 1) Build the LOWEST core from pool8 for that category.
            List<Card> core = pickLowestCoreFor(pool8, mRank);
            if (core == null || core.isEmpty()) {
                // Shouldn't happen if proposedMiddle had that category in pool8; fallback to original
                List<Card> front = subtract(pool8, proposedMiddle);
                return new Partition(new ArrayList<>(back), new ArrayList<>(proposedMiddle), front);
            }

            // 2) Fill with LOWEST remaining cards to reach 5.
            List<Card> remaining = subtract(pool8, core);
            remaining.sort(Comparator.comparingInt(Card::getRank)); // ascending
            int need = 5 - core.size();
            List<Card> fillers = new ArrayList<>();
            for (int i = 0; i < need && i < remaining.size(); i++) {
                fillers.add(remaining.get(i));
            }

            List<Card> newMiddle = new ArrayList<>(core);
            newMiddle.addAll(fillers);

            // 3) Front = TOP 3 of what's left
            List<Card> afterMiddle = subtract(pool8, newMiddle);
            afterMiddle.sort(Comparator.comparingInt(Card::getRank)); // ascending
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

        // Not a low-group middle: keep original, and front = leftovers.
        List<Card> front = subtract(pool8, proposedMiddle);
        return new Partition(new ArrayList<>(back), new ArrayList<>(proposedMiddle), front);
    }

    /**
     * From the 8-card pool (not in Back), pick the LOWEST possible core for the given category:
     *  - PAIR:    pick the lowest rank with count>=2 (two cards).
     *  - TWO_PAIR:pick the two lowest ranks with count>=2 (four cards).
     *  - TRIPS:   pick the lowest rank with count>=3 (three cards).
     */
    private List<Card> pickLowestCoreFor(List<Card> pool8, HandEvaluator.HandRank category) {
        // Count ranks
        int[] counts = new int[15]; // ranks 2..14
        for (Card c : pool8) counts[c.getRank()]++;

        if (category == HandEvaluator.HandRank.THREE_OF_A_KIND) {
            for (int r = 2; r <= 14; r++) {
                if (counts[r] >= 3) {
                    return takeNOfRank(pool8, r, 3);
                }
            }
            return null;
        } else if (category == HandEvaluator.HandRank.TWO_PAIR) {
            Integer first = null, second = null;
            for (int r = 2; r <= 14; r++) {
                if (counts[r] >= 2) {
                    if (first == null) first = r;
                    else { second = r; break; }
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
            for (int r = 2; r <= 14; r++) {
                if (counts[r] >= 2) {
                    return takeNOfRank(pool8, r, 2);
                }
            }
            return null;
        }
        return null;
    }

    private List<Card> takeNOfRank(List<Card> cards, int rank, int n) {
        List<Card> out = new ArrayList<>();
        for (Card c : cards) {
            if (c.getRank() == rank) {
                out.add(c);
                if (out.size() == n) break;
            }
        }
        return out;
    }

    /**
     * AI comparator for sorting 5-card hands when choosing candidates.
     * Keeps normal poker strength EXCEPT:
     *  - If BOTH hands are FULL_HOUSE and have the SAME trips rank,
     *    it prefers the LOWER pair rank (e.g., AAA33 is preferred over AAAQQ).
     *
     * Returns >0 if h1 is preferred over h2, <0 if h2 is preferred, 0 if equal.
     */
    private int aiCompareHandsForSorting(Hand h1, Hand h2) {
        if (h1.getCards().size() == 5 && h2.getCards().size() == 5) {
            HandEvaluator.HandRank r1 = HandEvaluator.evaluateFiveCardHand(h1);
            HandEvaluator.HandRank r2 = HandEvaluator.evaluateFiveCardHand(h2);

            if (r1 == HandEvaluator.HandRank.FULL_HOUSE && r2 == HandEvaluator.HandRank.FULL_HOUSE) {
                int t1 = getTripsRankIfFullHouse(h1);
                int t2 = getTripsRankIfFullHouse(h2);
                if (t1 != t2) {
                    // Higher trips is still preferred
                    return Integer.compare(t1, t2);
                }
                // Same trips → prefer LOWER pair
                int p1 = getPairRankIfFullHouse(h1, /*lowest*/ true);
                int p2 = getPairRankIfFullHouse(h2, /*lowest*/ true);
                // If p1 < p2, we want h1 preferred → return positive
                return Integer.compare(p2, p1);
            }
        }
        // Default to standard poker strength
        return HandEvaluator.compareHands(h1, h2);
    }

    private int getTripsRankIfFullHouse(Hand h) {
        return h.getRankCounts().entrySet().stream()
                .filter(e -> e.getValue() == 3L)
                .mapToInt(e -> e.getKey())
                .findFirst()
                .orElse(-1);
    }

    private int getPairRankIfFullHouse(Hand h, boolean lowest) {
        return h.getRankCounts().entrySet().stream()
                .filter(e -> e.getValue() == 2L)
                .mapToInt(e -> e.getKey())
                .boxed()
                .sorted(lowest ? Integer::compareTo : Comparator.reverseOrder())
                .findFirst()
                .orElse(-1);
    }

    /**
     * Attempt a small swap to ensure Back > Middle if equal (rare edge case).
     * Try swapping one low kicker from Back with one high kicker from Middle.
     */
    private boolean tryPromoteBackOverMiddle(Partition p) {
        Hand backH = new Hand(new ArrayList<>(p.back));
        Hand midH = new Hand(new ArrayList<>(p.middle));
        backH.sortCardsByRank();
        midH.sortCardsByRank();

        if (HandEvaluator.compareHands(backH, midH) > 0) return true; // already strict

        // Try swapping each card i in back with each card j in middle
        for (int i = 0; i < p.back.size(); i++) {
            for (int j = 0; j < p.middle.size(); j++) {
                // swap
                Card bi = p.back.get(i);
                Card mj = p.middle.get(j);
                p.back.set(i, mj);
                p.middle.set(j, bi);

                Hand newBack = new Hand(new ArrayList<>(p.back));
                Hand newMid = new Hand(new ArrayList<>(p.middle));
                newBack.sortCardsByRank();
                newMid.sortCardsByRank();

                int cmp = HandEvaluator.compareHands(newBack, newMid);
                if (cmp > 0) {
                    return true; // fixed
                }

                // revert
                p.back.set(i, bi);
                p.middle.set(j, mj);
            }
        }
        return false;
    }

    /**
     * Generate all 5-card combinations from a list.
     * Returns lists of *references* to the same Card objects (important for subtraction by identity).
     */
    private List<List<Card>> combinations5(List<Card> cards) {
        List<List<Card>> result = new ArrayList<>();
        int n = cards.size();
        if (n < 5) return result;

        // Simple iterative 5 nested loops (n is small: 13 or 8) — fast and avoids recursion overhead.
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

    /**
     * Subtracts 'toRemove' from 'from' by object identity (not by value),
     * returning a new list with the remaining cards in the same order as 'from'.
     */
    private List<Card> subtract(List<Card> from, List<Card> toRemove) {
        List<Card> result = new ArrayList<>(from);
        // remove by identity: since Card has no equals/hashCode override, this is correct
        for (Card r : toRemove) {
            // remove the first occurrence (same object reference)
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
