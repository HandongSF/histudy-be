package edu.handong.csee.histudy;

import io.jsonwebtoken.Claims;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/tests")
public class TestController {

    @GetMapping
    public Map<String, String> test(@RequestAttribute Claims claims) {
        Map<String, String> response = new HashMap<>();

        for (var entry : claims.entrySet()) {
            response.put(entry.getKey(), entry.getValue().toString());
        }
        return response;
    }
}
