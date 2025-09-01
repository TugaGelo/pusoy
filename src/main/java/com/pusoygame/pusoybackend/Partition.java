package com.pusoygame.pusoybackend;

import java.util.List;

/**
 * Small container for a 13-card partition into back(5), middle(5), front(3).
 */
public class Partition {
    public final List<Card> back;   // 5
    public final List<Card> middle; // 5
    public final List<Card> front;  // 3

    public Partition(List<Card> back, List<Card> middle, List<Card> front) {
        this.back = back;
        this.middle = middle;
        this.front = front;
    }

    @Override
    public String toString() {
        return "Partition{BACK=" + back + ", MIDDLE=" + middle + ", FRONT=" + front + "}";
    }
}
