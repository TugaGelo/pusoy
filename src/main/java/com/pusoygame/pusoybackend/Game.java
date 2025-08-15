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
     * >>> NEW AI <<<
     *
     * Build two 5-card hands (Back and Middle) from the AI's 13-card hand so that:
     *   Back > Middle (strict), and the leftover 3-card Front is maximized.
     * This uses full poker recognition via your HandEvaluator.
     */
    public void setAIHands(Player player) {
        // Defensive: ensure we have 13 cards.
        if (player == null || player.getHand() == null || player.getHand().getCards().size() != 13) {
            System.out.println("AI setup failed: invalid hand size.");
            // Fallback: do nothing.
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

        // Final safe fallback (original greedy split) — should almost never foul in practice.
        List<Card> fallback = new ArrayList<>(pool);
        Hand backHand = new Hand(fallback.subList(8, 13));   // strongest 5 by rank
        Hand middleHand = new Hand(fallback.subList(3, 8));  // next 5
        Hand frontHand = new Hand(fallback.subList(0, 3));   // weakest 3

        if (!setPlayerHands(player, frontHand, middleHand, backHand)) {
            // Last-ditch: nudge one card from middle to back.
            List<Card> midCopy = new ArrayList<>(middleHand.getCards());
            List<Card> backCopy = new ArrayList<>(backHand.getCards());
            if (!midCopy.isEmpty()) {
                backCopy.set(0, midCopy.remove(midCopy.size() - 1)); // swap a kicker
            }
            setPlayerHands(player, frontHand, new Hand(midCopy), new Hand(backCopy));
        }
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
     * Pick the best partition (Back, Middle, Front) using full poker evaluation.
     * - Enumerate all 5-card combos (C(13,5) = 1287).
     * - For each, enumerate all 5-card combos from the remaining 8 (C(8,5) = 56).
     * - Order the two 5-card hands so Back > Middle (strict). Skip equal (would foul).
     * - Leftover 3 are Front.
     * - Choose the candidate with lexicographic priority: Back (strongest), then Middle, then Front.
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
                // Determine which should be Back and which should be Middle
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

                // Front = leftover 3 cards after removing both fives
                List<Card> rem3 = subtract(rem8, middle);
                if (rem3.size() != 3) {
                    // Defensive: something went wrong if we don't have exactly 3 leftover
                    continue;
                }

                candidates.add(new Partition(new ArrayList<>(back), new ArrayList<>(middle), new ArrayList<>(rem3)));
            }
        }

        if (candidates.isEmpty()) {
            return null;
        }

        // Choose best candidate by comparing Back first, then Middle, then Front.
        candidates.sort((p1, p2) -> {
            int backCmp = HandEvaluator.compareHands(new Hand(p1.back), new Hand(p2.back));
            if (backCmp != 0) return -backCmp; // stronger back first

            int midCmp = HandEvaluator.compareHands(new Hand(p1.middle), new Hand(p2.middle));
            if (midCmp != 0) return -midCmp; // stronger middle next

            // For 3-card front comparison, HandEvaluator.compareHands handles 3v3 fine.
            int frontCmp = HandEvaluator.compareHands(new Hand(p1.front), new Hand(p2.front));
            return -frontCmp; // stronger front last
        });

        // Return the top partition
        return candidates.get(0);
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
