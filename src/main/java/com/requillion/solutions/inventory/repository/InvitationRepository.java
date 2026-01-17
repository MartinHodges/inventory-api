package com.requillion.solutions.inventory.repository;

import com.requillion.solutions.inventory.model.Invitation;
import com.requillion.solutions.inventory.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, UUID> {

    Optional<Invitation> findByToken(String token);

    @Query("SELECT i FROM Invitation i WHERE i.inventory = :inventory AND i.email = :email AND i.acceptedAt IS NULL")
    Optional<Invitation> findPendingByInventoryAndEmail(@Param("inventory") Inventory inventory, @Param("email") String email);

    @Query("SELECT i FROM Invitation i WHERE i.inventory = :inventory AND i.acceptedAt IS NULL ORDER BY i.createdAt DESC")
    List<Invitation> findPendingByInventory(@Param("inventory") Inventory inventory);

    @Query("SELECT i FROM Invitation i WHERE i.email = :email AND i.acceptedAt IS NULL AND i.expiresAt > CURRENT_TIMESTAMP ORDER BY i.createdAt DESC")
    List<Invitation> findPendingByEmail(@Param("email") String email);

    List<Invitation> findByInventory(Inventory inventory);
}
