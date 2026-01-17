package com.requillion.solutions.inventory.dto;

import com.requillion.solutions.inventory.model.MemberRole;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class DashboardItemDTO {
    private UUID id;
    private String name;
    private String description;
    private boolean isOwner;
    private MemberRole userRole;
    private int itemCount;
    private DashboardItemType type;
    private String invitationToken;
    private Instant invitationExpiresAt;
    private String invitedBy;

    public enum DashboardItemType {
        INVENTORY,
        PENDING_INVITATION
    }

    // Constructor for inventory
    public DashboardItemDTO(UUID id, String name, String description, boolean isOwner, 
                           MemberRole userRole, int itemCount) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.isOwner = isOwner;
        this.userRole = userRole;
        this.itemCount = itemCount;
        this.type = DashboardItemType.INVENTORY;
    }

    // Constructor for pending invitation
    public DashboardItemDTO(UUID inventoryId, String inventoryName, String inventoryDescription,
                           MemberRole invitedRole, String invitationToken, Instant expiresAt, String invitedBy) {
        this.id = inventoryId;
        this.name = inventoryName;
        this.description = inventoryDescription;
        this.isOwner = false;
        this.userRole = invitedRole;
        this.itemCount = 0;
        this.type = DashboardItemType.PENDING_INVITATION;
        this.invitationToken = invitationToken;
        this.invitationExpiresAt = expiresAt;
        this.invitedBy = invitedBy;
    }
}