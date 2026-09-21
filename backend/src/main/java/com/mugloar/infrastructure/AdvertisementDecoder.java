package com.mugloar.infrastructure;

import com.mugloar.domain.Advertisement;
import com.mugloar.domain.Probability;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

final class AdvertisementDecoder {
    private AdvertisementDecoder() {}

    static Advertisement decode(Advertisement ad) {
        if (!isEncrypted(ad.encrypted())) return ad;

        var base64Probability = decodeBase64(ad.probability());
        if (isKnownProbability(base64Probability)) {
            var adId = decodeBase64(ad.adId());
            var message = decodeBase64(ad.message());
            if (adId != null && message != null) {
                return decoded(ad, adId, message, base64Probability);
            }
        }

        var rot13Probability = rot13(ad.probability());
        if (isKnownProbability(rot13Probability)) {
            return decoded(ad, rot13(ad.adId()), rot13(ad.message()), rot13Probability);
        }

        return ad;
    }

    private static Advertisement decoded(Advertisement source, String adId, String message, String probability) {
        return new Advertisement(adId, message, source.reward(), source.expiresIn(), source.encrypted(), probability);
    }

    private static boolean isEncrypted(Object value) {
        return Boolean.TRUE.equals(value)
                || value instanceof Number number && number.intValue() != 0
                || value instanceof String text && ("1".equals(text) || "true".equalsIgnoreCase(text));
    }

    private static boolean isKnownProbability(String value) {
        return value != null && Probability.fromLabel(value) != Probability.UNKNOWN;
    }

    private static String decodeBase64(String value) {
        if (value == null) return null;
        try {
            return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static String rot13(String value) {
        if (value == null) return null;
        var result = new StringBuilder(value.length());
        for (var character : value.toCharArray()) {
            if (character >= 'a' && character <= 'z') {
                result.append((char) ('a' + (character - 'a' + 13) % 26));
            } else if (character >= 'A' && character <= 'Z') {
                result.append((char) ('A' + (character - 'A' + 13) % 26));
            } else {
                result.append(character);
            }
        }
        return result.toString();
    }
}
