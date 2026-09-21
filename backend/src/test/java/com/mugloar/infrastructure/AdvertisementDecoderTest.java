package com.mugloar.infrastructure;

import com.mugloar.domain.Advertisement;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class AdvertisementDecoderTest {
    @Test
    void decodesBase64FieldsAndPreservesMetadata() {
        var encoded = new Advertisement(base64("safe_ad-1"), base64("Rescue the royal chicken"),
                120, 4, 1, base64("Piece of cake"));

        var decoded = AdvertisementDecoder.decode(encoded);

        assertThat(decoded)
                .extracting(Advertisement::adId, Advertisement::message, Advertisement::probability,
                        Advertisement::reward, Advertisement::expiresIn, Advertisement::encrypted)
                .containsExactly("safe_ad-1", "Rescue the royal chicken", "Piece of cake", 120, 4, 1);
    }

    @Test
    void fallsBackToRot13() {
        var encoded = new Advertisement(rot13("quest_42"), rot13("Guard the tower"),
                75, 2, true, rot13("Quite likely"));

        var decoded = AdvertisementDecoder.decode(encoded);

        assertThat(decoded.adId()).isEqualTo("quest_42");
        assertThat(decoded.message()).isEqualTo("Guard the tower");
        assertThat(decoded.probability()).isEqualTo("Quite likely");
    }

    @Test
    void leavesUnknownEncryptionUntouched() {
        var encoded = new Advertisement("invalid+id=", "ciphertext", 10, 1, 1, "not-a-known-cipher");
        assertThat(AdvertisementDecoder.decode(encoded)).isSameAs(encoded);
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
