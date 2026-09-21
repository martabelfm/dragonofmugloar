package com.mugloar.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.mugloar.domain.Advertisement;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class AdvertisementDeserializerTest {
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new SimpleModule().addDeserializer(Advertisement.class, new AdvertisementDeserializer()));

    @Test
    void decodesBase64FieldsAndPreservesMetadata() throws Exception {
        var json = """
                {"adId":"%s","message":"%s","reward":120,"expiresIn":4,"encrypted":1,"probability":"%s"}
                """.formatted(base64("safe_ad-1"), base64("Rescue the royal chicken"), base64("Piece of cake"));

        var decoded = mapper.readValue(json, Advertisement.class);

        assertThat(decoded)
                .extracting(Advertisement::adId, Advertisement::message, Advertisement::probability,
                        Advertisement::reward, Advertisement::expiresIn)
                .containsExactly("safe_ad-1", "Rescue the royal chicken", "Piece of cake", 120, 4);
    }

    @Test
    void fallsBackToRot13() throws Exception {
        var json = """
                {"adId":"%s","message":"%s","reward":75,"expiresIn":2,"encrypted":true,"probability":"%s"}
                """.formatted(rot13("quest_42"), rot13("Guard the tower"), rot13("Quite likely"));

        var decoded = mapper.readValue(json, Advertisement.class);

        assertThat(decoded.adId()).isEqualTo("quest_42");
        assertThat(decoded.message()).isEqualTo("Guard the tower");
        assertThat(decoded.probability()).isEqualTo("Quite likely");
    }

    @Test
    void leavesUnknownEncryptionUntouched() throws Exception {
        var json = """
                {"adId":"invalid+id=","message":"ciphertext","reward":10,"expiresIn":1,"encrypted":1,\
                "probability":"not-a-known-cipher"}
                """;

        var decoded = mapper.readValue(json, Advertisement.class);

        assertThat(decoded.adId()).isEqualTo("invalid+id=");
        assertThat(decoded.message()).isEqualTo("ciphertext");
        assertThat(decoded.probability()).isEqualTo("not-a-known-cipher");
    }

    private static String base64(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String rot13(String value) {
        var result = new StringBuilder(value.length());
        for (var character : value.toCharArray()) {
            if (character >= 'a' && character <= 'z') result.append((char) ('a' + (character - 'a' + 13) % 26));
            else if (character >= 'A' && character <= 'Z') result.append((char) ('A' + (character - 'A' + 13) % 26));
            else result.append(character);
        }
        return result.toString();
    }
}
