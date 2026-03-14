package com.portal.lostfound.service;

import com.portal.lostfound.model.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final JavaMailSender mailSender;

    @Autowired
    public NotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void notifyMatches(Item item, List<MatchingService.MatchResult> matches) {
        String ownerEmail = item.getPostedBy().getEmail();
        String ownerName  = item.getPostedBy().getFullName();

        StringBuilder sb = new StringBuilder();
        sb.append("Hello ").append(ownerName).append(",\n\n");
        sb.append("Great news! We found ").append(matches.size())
          .append(" potential match(es) for your ")
          .append(item.getType().name().toLowerCase())
          .append(" item: \"").append(item.getTitle()).append("\".\n\n");
        sb.append("Top matches:\n");

        int limit = Math.min(matches.size(), 5);
        for (int i = 0; i < limit; i++) {
            MatchingService.MatchResult m = matches.get(i);
            sb.append("  - ").append(m.getItem().getTitle())
              .append(" (").append(m.getScorePercent()).append("% match)")
              .append(" — ")
              .append(m.getItem().getLocationName() != null
                      ? m.getItem().getLocationName() : "location unknown")
              .append("\n");
        }

        sb.append("\nLog in to SmartLostFound to review and claim your item.\n\n");
        sb.append("— The SmartLostFound Team\n");

        sendEmail(ownerEmail, "Potential matches found for your item!", sb.toString());
    }

    @Async
    public void notifyClaimSubmitted(String ownerEmail, String ownerName,
                                     String itemTitle, String claimantName) {
        String body = "Hello " + ownerName + ",\n\n"
                + claimantName + " has submitted a claim on your item: \"" + itemTitle + "\".\n\n"
                + "Please log in to SmartLostFound to review the claim and approve or reject it.\n\n"
                + "— The SmartLostFound Team\n";

        sendEmail(ownerEmail, "New claim on your item: " + itemTitle, body);
    }

    @Async
    public void notifyClaimDecision(String claimantEmail, String claimantName,
                                    String itemTitle, boolean approved) {
        String status = approved ? "APPROVED" : "REJECTED";
        String detail = approved
                ? "Please coordinate with the item owner to arrange the return."
                : "If you believe this is a mistake, please contact our support team.";

        String body = "Hello " + claimantName + ",\n\n"
                + "Your claim on item \"" + itemTitle + "\" has been " + status + ".\n\n"
                + detail + "\n\n"
                + "Log in to SmartLostFound for more details.\n\n"
                + "— The SmartLostFound Team\n";

        sendEmail(claimantEmail, "Claim update: " + itemTitle + " — " + status, body);
    }

    // ── Private ────────────────────────────────────────────────

    private void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            message.setFrom("noreply@smartlostfound.com");
            mailSender.send(message);
            logger.info("Email sent to {}: {}", to, subject);
        } catch (Exception e) {
            logger.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
