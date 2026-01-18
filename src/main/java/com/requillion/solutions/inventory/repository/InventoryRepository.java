package com.requillion.solutions.inventory.repository;

import com.requillion.solutions.inventory.model.Inventory;
import com.requillion.solutions.inventory.model.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    List<Inventory> findByOwner(User owner);

    Optional<Inventory> findByOwnerAndId(User owner, UUID id);

    Optional<Inventory> findByOwnerAndName(User owner, String name);

    @Query("SELECT i FROM Inventory i WHERE i.owner = :user OR EXISTS " +
           "(SELECT m FROM InventoryMember m WHERE m.inventory = i AND m.user = :user AND m.status = 'ACTIVE')")
    List<Inventory> findAccessibleByUser(@Param("user") User user);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.id = :id")
    Optional<Inventory> findByIdWithLock(@Param("id") UUID id);
}
