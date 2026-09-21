package com.mugloar.web;

import com.mugloar.application.GameService;
import com.mugloar.application.GameView;
import com.mugloar.domain.StrategyMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/games")
public class GameController {
    private static final String ID_PATTERN = "[A-Za-z0-9_-]{1,64}";
    private final GameService service;

    public GameController(GameService service) {
        this.service = service;
    }

    @PostMapping
    public GameView start() { return service.start(); }

    @GetMapping("/{gameId}")
    public GameView get(@PathVariable @Pattern(regexp = ID_PATTERN) String gameId) {
        return service.get(gameId);
    }

    @PostMapping("/{gameId}/refresh")
    public GameView refresh(@PathVariable @Pattern(regexp = ID_PATTERN) String gameId) {
        return service.refresh(gameId);
    }

    @PostMapping("/{gameId}/ads/{adId}/solve")
    public GameView solve(
            @PathVariable @Pattern(regexp = ID_PATTERN) String gameId,
            @PathVariable @Pattern(regexp = ID_PATTERN) String adId) {
        return service.solve(gameId, adId);
    }

    @PostMapping("/{gameId}/shop/{itemId}/purchase")
    public GameView purchase(
            @PathVariable @Pattern(regexp = ID_PATTERN) String gameId,
            @PathVariable @Pattern(regexp = ID_PATTERN) String itemId) {
        return service.purchase(gameId, itemId);
    }

    @PostMapping("/{gameId}/reputation")
    public GameView investigate(@PathVariable @Pattern(regexp = ID_PATTERN) String gameId) {
        return service.investigate(gameId);
    }

    @PutMapping("/{gameId}/strategy-mode")
    public GameView updateStrategyMode(
            @PathVariable @Pattern(regexp = ID_PATTERN) String gameId,
            @Valid @RequestBody StrategyModeRequest request) {
        return service.updateStrategyMode(gameId, request.mode());
    }

    public record StrategyModeRequest(@jakarta.validation.constraints.NotNull StrategyMode mode) {}

    @PostMapping("/{gameId}/auto/step")
    public GameView autoStep(@PathVariable @Pattern(regexp = ID_PATTERN) String gameId) {
        return service.autoStep(gameId);
    }
}
