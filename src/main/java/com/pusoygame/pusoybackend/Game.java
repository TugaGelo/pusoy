package com.pusoygame.pusoybackend;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Game orchestrates dealing and AI hand setup using AIHandBuilder and AutoWinChecker.
 * This preserves existing behavior while delegating heavy logic to separate classes.
 */
public class Game {

    private List<Card> deck;
    private List<Player> players;
    private int currentPlayerIndex;

    public Game(List<Player> players) {
        this.players = players;
        this.deck = new ArrayList<>();
        initializeDeck();
        shuffleDeck();
        this.currentPlayerIndex = 0;
        dealCards();
    }

    private void initializeDeck() {
        String[] suits = {"Hearts", "Diamonds", "Clubs", "Spades"};
        for (String suit : suits) {
            for (Rank rank : Rank.values()) {
                deck.add(new Card(suit, rank.getValue()));
            }
        }
    }

    private void shuffleDeck() {
        Collections.shuffle(deck);
    }

    private void dealCards() {
        for (Player player : players) {
            List<Card> playerCardList = new ArrayList<>();
            for (int i = 0; i < 13; i++) playerCardList.add(deck.remove(0));
            player.setHand(new Hand(playerCardList));
        }
    }

    public void sortPlayerHand(Player player) {
        if (player != null && player.getHand() != null) {
            Collections.sort(player.getHand().getCards(), Comparator.comparingInt(Card::getRank));
        }
    }

    public boolean checkFoul(Hand front, Hand middle, Hand back) {
        boolean backIsStrongerThanMiddle = HandEvaluator.compareHands(back, middle) > 0;
        boolean middleIsStrongerThanFront = HandEvaluator.compareHands(middle, front) > 0;
        return !(backIsStrongerThanMiddle && middleIsStrongerThanFront);
    }

    public boolean setPlayerHands(Player player, Hand front, Hand middle, Hand back) {
        if (checkFoul(front, middle, back)) return false;
        player.setHands(front, middle, back);
        return true;
    }

    /**
     * Set AI hands for the player.
     *  - First check for auto-wins (calls AutoWinChecker). If present, we still build a partition but you could
     *    choose to treat those differently (e.g. mark player as auto-win).
     *  - Otherwise use AIHandBuilder to produce a partition and set hands.
     */
    public void setAIHands(Player player) {
        if (player == null || player.getHand() == null || player.getHand().getCards().size() != 13) {
            System.out.println("AI setup failed: invalid hand size.");
            return;
        }

        // check auto wins
        AutoWinChecker.AutoWinType aw = AutoWinChecker.detectAutoWin(player.getHand().getCards());
        if (aw != AutoWinChecker.AutoWinType.NONE) {
            System.out.println(player.getName() + " has auto-win: " + aw);
            // you might want special handling here (e.g., award immediately). For now, we'll still split normally.
        }

        // Build partition using AIHandBuilder
        Partition p = AIHandBuilder.buildBestPartition(player.getHand().getCards());
        if (p != null) {
            boolean ok = setPlayerHands(player, new Hand(p.front), new Hand(p.middle), new Hand(p.back));
            if (!ok) {
                // fallback: try naive split (strongest back, next middle, last front)
                List<Card> pool = new ArrayList<>(player.getHand().getCards());
                pool.sort(Comparator.comparingInt(Card::getRank));
                Hand backHand = new Hand(new ArrayList<>(pool.subList(8, 13)));
                List<Card> pool8 = subtract(pool, backHand.getCards());
                Hand middleHand = new Hand(new ArrayList<>(pool8.subList(3, 8)));
                List<Card> frontCards = subtract(pool8, middleHand.getCards());
                setPlayerHands(player, new Hand(frontCards), middleHand, backHand);
                System.out.println(player.getName() + " AI fallback split applied.");
            } else {
                System.out.println(player.getName() + " (AI) set hands: BACK=" + new Hand(p.back)
                        + ", MIDDLE=" + new Hand(p.middle) + ", FRONT=" + new Hand(p.front));
            }
            return;
        }

        // If AI builder failed, fallback to simple split
        List<Card> pool = new ArrayList<>(player.getHand().getCards());
        pool.sort(Comparator.comparingInt(Card::getRank));
        Hand backHand = new Hand(new ArrayList<>(pool.subList(8, 13)));
        List<Card> pool8 = subtract(pool, backHand.getCards());
        Hand middleHand = new Hand(new ArrayList<>(pool8.subList(3, 8)));
        List<Card> frontCards = subtract(pool8, middleHand.getCards());
        setPlayerHands(player, new Hand(frontCards), middleHand, backHand);
        System.out.println(player.getName() + " (AI) naive split applied.");
    }

    // expose comparator showdown function unchanged
    public void compareAllPlayerHands() {
        System.out.println("\n--- Starting the Showdown ---");
        Player humanPlayer = players.get(0);
        for (int i = 1; i < players.size(); i++) {
            Player aiPlayer = players.get(i);
            if (humanPlayer.getBackHand() != null && aiPlayer.getBackHand() != null) {
                System.out.println("Comparing hands for " + humanPlayer.getName() + " vs " + aiPlayer.getName() + ":");
                int frontComparison = HandEvaluator.compareHands(humanPlayer.getFrontHand(), aiPlayer.getFrontHand());
                if (frontComparison > 0) System.out.println("- " + humanPlayer.getName() + "'s front hand wins!");
                else if (frontComparison < 0) System.out.println("- " + aiPlayer.getName() + "'s front hand wins!");
                else System.out.println("- Front hands are a tie!");

                int middleComparison = HandEvaluator.compareHands(humanPlayer.getMiddleHand(), aiPlayer.getMiddleHand());
                if (middleComparison > 0) System.out.println("- " + humanPlayer.getName() + "'s middle hand wins!");
                else if (middleComparison < 0) System.out.println("- " + aiPlayer.getName() + "'s middle hand wins!");
                else System.out.println("- Middle hands are a tie!");

                int backComparison = HandEvaluator.compareHands(humanPlayer.getBackHand(), aiPlayer.getBackHand());
                if (backComparison > 0) System.out.println("- " + humanPlayer.getName() + "'s back hand wins!");
                else if (backComparison < 0) System.out.println("- " + aiPlayer.getName() + "'s back hand wins!");
                else System.out.println("- Back hands are a tie!");
            }
        }
    }

    // small identity subtract helper used by fallback
    private List<Card> subtract(List<Card> from, List<Card> toRemove) {
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

    public List<Player> getPlayers() { return players; }
    public List<Card> getDeck() { return deck; }
}
