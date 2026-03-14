package com.portal.lostfound.repository;

import com.portal.lostfound.model.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    Page<Item> findByStatus(Item.ItemStatus status, Pageable pageable);

    Page<Item> findByTypeAndStatus(Item.ItemType type, Item.ItemStatus status, Pageable pageable);

    Page<Item> findByPostedById(Long userId, Pageable pageable);

    Page<Item> findByTypeAndStatusAndCategory(
            Item.ItemType type, Item.ItemStatus status, String category, Pageable pageable);

    @Query("SELECT i FROM Item i WHERE i.status = 'ACTIVE' AND (" +
           "LOWER(i.title) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(i.description) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(i.brand) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(i.model) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Item> searchItems(@Param("q") String query, Pageable pageable);

    @Query("SELECT i FROM Item i WHERE i.type = :type AND i.status = 'ACTIVE' " +
           "AND i.category = :category " +
           "AND i.dateOccurred BETWEEN :from AND :to")
    List<Item> findMatchCandidates(
            @Param("type") Item.ItemType type,
            @Param("category") String category,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("SELECT COUNT(i) FROM Item i WHERE i.type = :type AND i.status = 'ACTIVE'")
    long countByTypeAndActive(@Param("type") Item.ItemType type);
}
