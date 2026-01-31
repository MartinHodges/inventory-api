package com.requillion.solutions.inventory.repository;

import com.requillion.solutions.inventory.model.ClaimStatus;
import com.requillion.solutions.inventory.model.Item;
import com.requillion.solutions.inventory.model.ItemClaim;
import com.requillion.solutions.inventory.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ItemClaimRepository extends JpaRepository<ItemClaim, UUID> {

    List<ItemClaim> findByItem(Item item);

    Optional<ItemClaim> findByItemAndUser(Item item, User user);

    Optional<ItemClaim> findByItemAndStatus(Item item, ClaimStatus status);

    boolean existsByItemAndStatus(Item item, ClaimStatus status);

    long countByItem(Item item);

    @Query("SELECT c FROM ItemClaim c WHERE c.item.id IN :itemIds")
    List<ItemClaim> findByItemIdIn(@Param("itemIds") List<UUID> itemIds);

    @Query("SELECT c FROM ItemClaim c JOIN FETCH c.item i " +
           "LEFT JOIN FETCH i.category " +
           "WHERE i.inventory.id = :inventoryId AND c.user = :user AND i.isDeleted = false " +
           "ORDER BY i.referenceNumber ASC")
    List<ItemClaim> findByUserAndInventoryId(@Param("user") User user, @Param("inventoryId") UUID inventoryId);
}
