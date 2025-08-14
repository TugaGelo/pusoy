package com.pusoygame.pusoybackend;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    private final AtomicReference<Game> currentGame = new AtomicReference<>();

    // A method for primary endpoint for our frontend.
    @GetMapping("/game")
    public Game getGameState() {
        Game game = currentGame.get();
        if (game == null) {
            game = initializeNewGame();
            currentGame.set(game);
        }
        return currentGame.get();
    }

    // This new @PostMapping endpoint receives the human player's hand arrangement.
    @PostMapping("/game/set-hands")
    public ResponseEntity<Game> setHumanHands(@RequestBody HandSubmission submission) {
        Game game = currentGame.get();
        Player humanPlayer = game.getPlayers().stream()
                .filter(p -> p.getId().equals(submission.getPlayerId()))
                .findFirst()
                .orElse(null);

        if (humanPlayer == null) {
            return ResponseEntity.badRequest().body(game);
        }

        Hand front = new Hand(submission.getFrontHand());
        Hand middle = new Hand(submission.getMiddleHand());
        Hand back = new Hand(submission.getBackHand());

        if (game.setPlayerHands(humanPlayer, front, middle, back)) {
            game.compareAllPlayerHands();
            currentGame.set(game);
            return ResponseEntity.ok(game);
        } else {
            return ResponseEntity.badRequest().body(game);
        }
    }

    // endpoint will reset the game state and deal new hands.
    @GetMapping("/game/new")
    public Game newGame() {
        Game newGame = initializeNewGame();
        currentGame.set(newGame);
        return newGame;
    }

    // A method to initialize a new game.
    private Game initializeNewGame() {
        List<Player> players = new ArrayList<>();
        players.add(new Player("Human"));
        players.add(new Player("AI 1"));
        players.add(new Player("AI 2"));
        players.add(new Player("AI 3"));
        Game game = new Game(players);
        
        for (int i = 1; i < players.size(); i++) {
            game.setAIHands(players.get(i));
        }
        
        return game;
    }
}
