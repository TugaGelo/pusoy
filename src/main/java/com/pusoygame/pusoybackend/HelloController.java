package com.pusoygame.pusoybackend;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/game")
    public Game startGame() {

        // --- Phase 1: Set up the players ---
        List<Player> players = new ArrayList<>();
        players.add(new Player("Human"));
        players.add(new Player("AI 1"));
        players.add(new Player("AI 2"));
        players.add(new Player("AI 3"));
        
        // --- Phase 2: Create a new game instance ---
        Game game = new Game(players);

        // --- Phase 3: Have the AI players set their hands ---
        for (int i = 1; i < players.size(); i++) {
            Player aiPlayer = players.get(i);
            game.setAIHands(aiPlayer);
        }
        
        // --- Phase 4: Compare all players' hands (The Showdown) ---
        game.compareAllPlayerHands();

        // --- Phase 5: Return the game state ---
        return game;
    }
}
