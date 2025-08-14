package com.pusoygame.pusoybackend;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// The @RestController annotation tells Spring that this class will handle incoming web requests.
@RestController
public class HelloController {

    // This method handles GET requests to the root URL ("/").
    @GetMapping("/")
    public String hello() {
        return "Hello from your Pusoy Game Backend! Go to /game to start playing.";
    }

    // The @GetMapping annotation maps HTTP GET requests to this method.
    // The path "/game" means this method will be triggered when a user accesses that URL.
    @GetMapping("/game")
    public Game startGame() {
        // --- Phase 1: Set up the players ---
        // Create a list of players, including one human and three AI opponents.
        List<Player> players = new ArrayList<>();
        players.add(new Player("Human"));
        players.add(new Player("AI 1"));
        players.add(new Player("AI 2"));
        players.add(new Player("AI 3"));
        
        // --- Phase 2: Create a new game instance ---
        // The Game class constructor automatically initializes and deals cards to the players.
        Game game = new Game(players);

        // --- Phase 3: Have the AI players set their hands ---
        // This is the new part of the code. We loop through the players.
        for (int i = 1; i < players.size(); i++) { // We start at index 1 to skip the human player.
            Player aiPlayer = players.get(i);
            game.setAIHands(aiPlayer);
        }
        
        // --- Phase 4: Return the game state ---
        // Spring Boot automatically converts this Game object into a JSON response.
        return game;
    }
}
