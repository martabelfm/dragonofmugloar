package com.mugloar.infrastructure;

import com.mugloar.application.GamePort;
import com.mugloar.domain.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import java.net.http.HttpClient;
import java.util.Arrays;
import java.util.List;

@Component
public class MugloarClient implements GamePort {
    private final RestClient client;

    public MugloarClient(RestClient.Builder builder, MugloarProperties properties) {
        var httpClient = HttpClient.newBuilder().connectTimeout(properties.connectTimeout()).build();
        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());
        var mapper = JsonMapper.builder()
                .addModule(new SimpleModule().addDeserializer(Advertisement.class, new AdvertisementDeserializer()))
                .build();
        this.client = builder.baseUrl(properties.baseUrl()).requestFactory(requestFactory)
                .configureMessageConverters(converters ->
                        converters.withJsonConverter(new JacksonJsonHttpMessageConverter(mapper)))
                .build();
    }

    @Override
    public PlayerState startGame() {
        var response = post("/api/v2/game/start", StartResponse.class);
        return new PlayerState(response.gameId(), response.lives(), response.gold(), response.level(),
                response.score(), response.highScore(), response.turn());
    }

    @Override
    public List<Advertisement> getAds(String gameId) {
        return list(get("/api/v2/{gameId}/messages", Advertisement[].class, gameId));
    }

    @Override
    public List<ShopItem> getShop(String gameId) {
        return list(get("/api/v2/{gameId}/shop", ShopItem[].class, gameId));
    }

    @Override
    public SolveOutcome solve(String gameId, String adId) {
        return post("/api/v2/{gameId}/solve/{adId}", SolveOutcome.class, gameId, adId);
    }

    @Override
    public PurchaseOutcome purchase(String gameId, String itemId) {
        var response = post("/api/v2/{gameId}/shop/buy/{itemId}", PurchaseResponse.class, gameId, itemId);
        return new PurchaseOutcome(response.shoppingSuccess(), response.gold(), response.lives(),
                response.level(), response.turn());
    }

    @Override
    public Reputation investigate(String gameId) {
        return post("/api/v2/{gameId}/investigate/reputation", Reputation.class, gameId);
    }

    private <T> T get(String uri, Class<T> type, Object... variables) {
        try {
            return requireBody(client.get().uri(uri, variables).retrieve().body(type));
        } catch (RestClientException exception) {
            throw translate(exception);
        }
    }

    private <T> T post(String uri, Class<T> type, Object... variables) {
        try {
            return requireBody(client.post().uri(uri, variables).retrieve().body(type));
        } catch (RestClientException exception) {
            throw translate(exception);
        }
    }

    private static <T> List<T> list(T[] values) {
        return values == null ? List.of() : Arrays.asList(values);
    }

    private static <T> T requireBody(T body) {
        if (body == null) throw new UpstreamGameException("Mugloar returned an empty response.", null);
        return body;
    }

    private static UpstreamGameException translate(RestClientException exception) {
        return new UpstreamGameException("The Mugloar service could not complete the request.", exception);
    }

    private record StartResponse(String gameId, int lives, int gold, int level, int score, int highScore, int turn) {}
    private record PurchaseResponse(boolean shoppingSuccess, int gold, int lives, int level, int turn) {}
}
