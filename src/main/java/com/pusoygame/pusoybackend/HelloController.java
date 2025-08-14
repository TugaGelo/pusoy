package com.pusoygame.pusoybackend;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// The @RestController annotation tells Spring that this class will handle incoming web requests.
@RestController
public class HelloController {

    // The @GetMapping annotation maps HTTP GET requests to this method.
    // The path "" means this method will be triggered when a user accesses the root URL.
    @GetMapping("/")
    public String index() {
        return "Hello from your Pusoy Game Backend!";
    }
}
