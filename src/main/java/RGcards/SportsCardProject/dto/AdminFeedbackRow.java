package RGcards.SportsCardProject.dto;

import RGcards.SportsCardProject.enums.FeedbackType;

import java.time.LocalDateTime;

public record AdminFeedbackRow(
        Long id,
        FeedbackType type,
        String content,
        String username,
        String email,
        LocalDateTime createdAt
) {}
