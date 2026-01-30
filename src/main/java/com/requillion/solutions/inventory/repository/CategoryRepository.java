package com.requillion.solutions.inventory.repository;

import com.requillion.solutions.inventory.model.Category;
import com.requillion.solutions.inventory.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findByInventoryOrderByDisplayOrderAsc(Inventory inventory);

    Optional<Category> findByInventoryAndId(Inventory inventory, UUID id);

    Optional<Category> findByInventoryAndName(Inventory inventory, String name);

    @Query("SELECT COALESCE(MAX(c.displayOrder), -1) FROM Category c WHERE c.inventory = :inventory")
    Integer findMaxDisplayOrder(@Param("inventory") Inventory inventory);

    long countByInventory(Inventory inventory);
}
