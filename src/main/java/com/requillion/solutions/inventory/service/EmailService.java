package com.requillion.solutions.inventory.service;

import com.requillion.solutions.inventory.model.Invitation;
import com.requillion.solutions.inventory.model.InventoryMember;
import com.requillion.solutions.inventory.util.LoggerUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.base-url:http://localhost:3000}")
    private String baseUrl;

    @Value("${spring.mail.username:noreply@inventory.app}")
    private String fromEmail;

    @Async
    public void sendInvitation(Invitation invitation, String inventoryName, String inviterName) {
        String acceptUrl = baseUrl + "/invitations/" + invitation.getToken();

        String subject = "You've been invited to " + inventoryName;
        String body = String.format("""
            Hi,

            %s has invited you to collaborate on "%s" as a %s.

            Click here to accept: %s

            This invitation expires in 7 days.

            If you didn't expect this invitation, you can safely ignore this email.
            """,
                inviterName,
                inventoryName,
                formatRole(invitation.getRole().name()),
                acceptUrl
        );

        sendEmail(invitation.getEmail(), subject, body);
        LoggerUtil.info(log, "Sent invitation email to %s for inventory %s",
                invitation.getEmail(), invitation.getInventory().getId());
    }

    @Async
    public void sendMemberApproved(InventoryMember member, String inventoryName) {
        String inventoryUrl = baseUrl + "/inventories/" + member.getInventory().getId();

        String subject = "Your access to " + inventoryName + " has been approved";
        String body = String.format("""
            Hi %s,

            Your request to access "%s" has been approved.

            You now have %s access to this inventory.

            Click here to view: %s
            """,
                member.getUser().getFirstName(),
                inventoryName,
                formatRole(member.getRole().name()),
                inventoryUrl
        );

        sendEmail(member.getUser().getEmail(), subject, body);
        LoggerUtil.info(log, "Sent approval notification to %s for inventory %s",
                member.getUser().getEmail(), member.getInventory().getId());
    }

    @Async
    public void sendNewMemberNotification(InventoryMember newMember, String adminEmail, String inventoryName) {
        String settingsUrl = baseUrl + "/inventories/" + newMember.getInventory().getId() + "/settings";

        String subject = "New member request for " + inventoryName;
        String body = String.format("""
            Hi,

            %s %s (%s) has accepted an invitation to "%s" and is pending your approval.

            Click here to review: %s
            """,
                newMember.getUser().getFirstName(),
                newMember.getUser().getLastName(),
                newMember.getUser().getEmail(),
                inventoryName,
                settingsUrl
        );

        sendEmail(adminEmail, subject, body);
        LoggerUtil.info(log, "Sent new member notification to admin %s for inventory %s",
                adminEmail, newMember.getInventory().getId());
    }

    private void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            LoggerUtil.error(log, "Failed to send email to %s: %s", to, e.getMessage());
        }
    }

    private String formatRole(String role) {
        return switch (role) {
            case "ADMIN" -> "Admin";
            case "CLAIMANT" -> "Claimant";
            case "VIEWER" -> "Viewer";
            default -> role;
        };
    }
}
