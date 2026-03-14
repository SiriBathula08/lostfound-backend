package com.portal.lostfound.service;

import com.portal.lostfound.model.Item;
import com.portal.lostfound.repository.ItemRepository;
import com.portal.lostfound.util.MatchingAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class MatchingService {

    private static final Logger logger = LoggerFactory.getLogger(MatchingService.class);

    private static final double MATCH_THRESHOLD = 0.40;
    private static final int    DATE_WINDOW_DAYS = 30;

    private final ItemRepository itemRepository;
    private final MatchingAlgorithm algorithm;
    private final NotificationService notificationService;

    @Autowired
    public MatchingService(ItemRepository itemRepository,
                           MatchingAlgorithm algorithm,
                           NotificationService notificationService) {
        this.itemRepository      = itemRepository;
        this.algorithm           = algorithm;
        this.notificationService = notificationService;
    }

    /**
     * Finds potential matches for the given item and notifies the owner.
     * Runs asynchronously so it does not block the HTTP response.
     */
    @Async
    @Transactional(readOnly = true)
    public void findAndNotifyMatches(Item newItem) {
        List<MatchResult> matches = findMatches(newItem);
        if (!matches.isEmpty()) {
            logger.info("Found {} potential match(es) for item [id={}]",
                    matches.size(), newItem.getId());
            notificationService.notifyMatches(newItem, matches);
        }
    }

    /**
     * Returns a ranked list of match results above the threshold.
     */
    @Transactional(readOnly = true)
    public List<MatchResult> findMatches(Item item) {
        Item.ItemType oppositeType = (item.getType() == Item.ItemType.LOST)
                ? Item.ItemType.FOUND
                : Item.ItemType.LOST;

        LocalDate baseDate = item.getDateOccurred() != null
                ? item.getDateOccurred() : LocalDate.now();
        LocalDate from = baseDate.minusDays(DATE_WINDOW_DAYS);
        LocalDate to   = baseDate.plusDays(DATE_WINDOW_DAYS);

        List<Item> candidates = itemRepository.findMatchCandidates(
                oppositeType, item.getCategory(), from, to);

        List<MatchResult> results = new ArrayList<>();
        for (Item candidate : candidates) {
            Item lost  = (item.getType() == Item.ItemType.LOST) ? item : candidate;
            Item found = (item.getType() == Item.ItemType.FOUND) ? item : candidate;
            double score = algorithm.calculateMatchScore(lost, found);
            if (score >= MATCH_THRESHOLD) {
                results.add(new MatchResult(candidate, score));
            }
        }

        results.sort(Comparator.comparingDouble(MatchResult::getScore).reversed());
        return results;
    }

    /**
     * Returns the top N matches for a given item.
     */
    @Transactional(readOnly = true)
    public List<MatchResult> getTopMatches(Item item, int limit) {
        List<MatchResult> all = findMatches(item);
        return all.subList(0, Math.min(limit, all.size()));
    }

    // ── MatchResult class ──────────────────────────────────────

    public static class MatchResult {

        private final Item item;
        private final double score;

        public MatchResult(Item item, double score) {
            this.item  = item;
            this.score = score;
        }

        public Item getItem() { return item; }
        public double getScore() { return score; }

        public int getScorePercent() {
            return (int) Math.round(score * 100);
        }
    }
}
