package com.requillion.solutions.inventory.repository;

import com.requillion.solutions.inventory.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryMemberRepository extends JpaRepository<InventoryMember, UUID> {

    List<InventoryMember> findByInventory(Inventory inventory);

    List<InventoryMember> findByUser(User user);

    Optional<InventoryMember> findByInventoryAndUser(Inventory inventory, User user);

    List<InventoryMember> findByInventoryAndStatus(Inventory inventory, MemberStatus status);

    boolean existsByInventoryAndUserAndStatusAndRoleIn(
            Inventory inventory, User user, MemberStatus status, List<MemberRole> roles);

    List<InventoryMember> findByInventoryAndRoleAndStatus(
            Inventory inventory, MemberRole role, MemberStatus status);

    List<InventoryMember> findByInventoryAndStatusAndRoleIn(
            Inventory inventory, MemberStatus status, List<MemberRole> roles);
}
