package com.requillion.solutions.inventory.repository;

import com.requillion.solutions.inventory.model.Category;
import com.requillion.solutions.inventory.model.Inventory;
import com.requillion.solutions.inventory.model.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ItemRepository extends JpaRepository<Item, UUID> {

    List<Item> findByInventoryAndIsDeletedFalseOrderByReferenceNumberAsc(Inventory inventory);

    Optional<Item> findByIdAndIsDeletedFalse(UUID id);

    Optional<Item> findByInventoryAndIdAndIsDeletedFalse(Inventory inventory, UUID id);

    @Query("SELECT COALESCE(MAX(i.referenceNumber), 0) FROM Item i WHERE i.inventory = :inventory")
    Integer findMaxReferenceNumber(@Param("inventory") Inventory inventory);

    long countByInventoryAndIsDeletedFalse(Inventory inventory);

    List<Item> findByCategoryAndIsDeletedFalseOrderByReferenceNumberAsc(Category category);

    List<Item> findByCategoryOrderByReferenceNumberAsc(Category category);

    Optional<Item> findByInventoryAndId(Inventory inventory, UUID id);

    long countByCategoryAndIsDeletedFalse(Category category);

    @Query("SELECT COALESCE(MAX(i.referenceNumber), 0) FROM Item i WHERE i.category = :category")
    Integer findMaxReferenceNumberByCategory(@Param("category") Category category);
}
