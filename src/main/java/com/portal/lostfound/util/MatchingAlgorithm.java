package com.portal.lostfound.util;

import com.portal.lostfound.model.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Weighted scoring algorithm that quantifies how well two items match.
 * Score is a value between 0.0 (no match) and 1.0 (perfect match).
 *
 * Weights:
 *   Category        10%
 *   Title words     20%
 *   Description     15%
 *   Color           15%
 *   Brand / Model   20%
 *   Date proximity  10%
 *   Location        10%
 */
@Component
public class MatchingAlgorithm {

    private static final Logger logger = LoggerFactory.getLogger(MatchingAlgorithm.class);

    private static final double W_CATEGORY    = 0.10;
    private static final double W_TITLE       = 0.20;
    private static final double W_DESCRIPTION = 0.15;
    private static final double W_COLOR       = 0.15;
    private static final double W_BRAND_MODEL = 0.20;
    private static final double W_DATE        = 0.10;
    private static final double W_LOCATION    = 0.10;

    public double calculateMatchScore(Item lost, Item found) {
        double score = 0.0;

        score += W_CATEGORY    * categoryScore(lost, found);
        score += W_TITLE       * textSimilarity(lost.getTitle(), found.getTitle());
        score += W_DESCRIPTION * textSimilarity(lost.getDescription(), found.getDescription());
        score += W_COLOR       * exactMatchScore(lost.getColor(), found.getColor());
        score += W_BRAND_MODEL * brandModelScore(lost, found);
        score += W_DATE        * dateScore(lost, found);
        score += W_LOCATION    * locationScore(lost, found);

        double rounded = Math.round(score * 100.0) / 100.0;
        logger.debug("Match score for items [{} vs {}]: {}", lost.getId(), found.getId(), rounded);
        return rounded;
    }

    // ── Individual scorers ─────────────────────────────────────

    private double categoryScore(Item a, Item b) {
        if (a.getCategory() == null || b.getCategory() == null) return 0.0;
        if (!a.getCategory().equalsIgnoreCase(b.getCategory())) return 0.0;
        if (a.getSubCategory() != null && b.getSubCategory() != null) {
            return a.getSubCategory().equalsIgnoreCase(b.getSubCategory()) ? 1.0 : 0.7;
        }
        return 1.0;
    }

    /**
     * Jaccard similarity on word tokens (case-insensitive).
     */
    private double textSimilarity(String a, String b) {
        if (isBlank(a) || isBlank(b)) return 0.0;
        Set<String> setA = tokenize(a);
        Set<String> setB = tokenize(b);
        Set<String> intersection = new HashSet<>(setA);
        intersection.retainAll(setB);
        long intersectionCount = intersection.size();
        long unionCount = setA.size() + setB.size() - intersectionCount;
        return unionCount == 0 ? 0.0 : (double) intersectionCount / unionCount;
    }

    private double exactMatchScore(String a, String b) {
        if (isBlank(a) || isBlank(b)) return 0.0;
        return a.trim().equalsIgnoreCase(b.trim()) ? 1.0 : 0.0;
    }

    private double brandModelScore(Item a, Item b) {
        double brandScore = exactMatchScore(a.getBrand(), b.getBrand());
        double modelScore = exactMatchScore(a.getModel(), b.getModel());
        if (brandScore == 1.0 && modelScore == 1.0) return 1.0;
        if (brandScore == 1.0) return 0.7;
        if (modelScore == 1.0) return 0.5;
        return 0.0;
    }

    /**
     * Score decays linearly: full score within 3 days, zero at 30 days.
     */
    private double dateScore(Item a, Item b) {
        if (a.getDateOccurred() == null || b.getDateOccurred() == null) return 0.5;
        long days = Math.abs(ChronoUnit.DAYS.between(a.getDateOccurred(), b.getDateOccurred()));
        if (days <= 3)  return 1.0;
        if (days >= 30) return 0.0;
        return 1.0 - (days - 3.0) / 27.0;
    }

    /**
     * Haversine distance: full score <= 1 km, zero at 50 km.
     */
    private double locationScore(Item a, Item b) {
        if (a.getLocationLat() == null || b.getLocationLat() == null) return 0.5;
        double distKm = haversineKm(
                a.getLocationLat(), a.getLocationLng(),
                b.getLocationLat(), b.getLocationLng());
        if (distKm <= 1.0)  return 1.0;
        if (distKm >= 50.0) return 0.0;
        return 1.0 - (distKm - 1.0) / 49.0;
    }

    // ── Helpers ────────────────────────────────────────────────

    private Set<String> tokenize(String text) {
        return Arrays.stream(text.toLowerCase().split("[\\s,;.!?]+"))
                .filter(t -> t.length() > 2)
                .collect(Collectors.toSet());
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
