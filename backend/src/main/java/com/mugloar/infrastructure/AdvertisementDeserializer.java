package com.mugloar.infrastructure;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mugloar.domain.Advertisement;
import com.mugloar.domain.Probability;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * The upstream API sometimes returns ad fields base64- or ROT13-encoded, signalled by an
 * "encrypted" flag whose JSON type varies (boolean, number, or string). Decoding happens here, at
 * the JSON boundary, so the rest of the application only ever sees plain Advertisement values.
 */
class AdvertisementDeserializer extends JsonDeserializer<Advertisement> {
    @Override
    public Advertisement deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        var mapper = (ObjectMapper) parser.getCodec();
        JsonNode node = mapper.readTree(parser);

        var encryptedNode = node.get("encrypted");
        var encrypted = encryptedNode == null || encryptedNode.isNull()
                ? null : mapper.treeToValue(encryptedNode, Object.class);

        var raw = new Advertisement(text(node, "adId"), text(node, "message"), node.path("reward").asInt(),
                node.path("expiresIn").asInt(), encrypted, text(node, "probability"));

        return isEncrypted(encrypted) ? decodeEncrypted(raw) : raw;
    }

    private static Advertisement decodeEncrypted(Advertisement ad) {
        var base64Probability = decodeBase64(ad.probability());
        if (isKnownProbability(base64Probability)) {
            var adId = decodeBase64(ad.adId());
            var message = decodeBase64(ad.message());
            if (adId != null && message != null) return withDecodedFields(ad, adId, message, base64Probability);
        }
        var rot13Probability = rot13(ad.probability());
        if (isKnownProbability(rot13Probability)) {
            return withDecodedFields(ad, rot13(ad.adId()), rot13(ad.message()), rot13Probability);
        }
        return ad;
    }

    private static Advertisement withDecodedFields(Advertisement source, String adId, String message,
                                                     String probability) {
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

    private static String text(JsonNode node, String field) {
        var value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
