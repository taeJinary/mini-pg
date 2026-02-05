package com.example.minipg.api.controller;

import java.util.Random;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fake-pg")
public class FakePgController {

    private final Random random = new Random();

    @PostMapping("/approve")
    public ResponseEntity<?> approve(@RequestParam(defaultValue = "success") String mode) {
        String resolvedMode = resolveMode(mode);

        switch (resolvedMode) {
            case "success":
                return ResponseEntity.ok(new PgSuccessResponse(UUID.randomUUID().toString()));
            case "fail":
                return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new PgFailResponse("PG_DECLINED", "Declined by fake PG"));
            case "timeout":
                sleep(3000);
                return ResponseEntity.ok(new PgSuccessResponse(UUID.randomUUID().toString()));
            default:
                return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new PgFailResponse("INVALID_MODE", "Invalid mode"));
        }
    }

    private String resolveMode(String mode) {
        if ("random".equalsIgnoreCase(mode)) {
            int pick = random.nextInt(3);
            if (pick == 0) {
                return "success";
            }
            if (pick == 1) {
                return "fail";
            }
            return "timeout";
        }
        return mode.toLowerCase();
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private record PgSuccessResponse(String pgTransactionId) {
    }

    private record PgFailResponse(String code, String message) {
    }
}
