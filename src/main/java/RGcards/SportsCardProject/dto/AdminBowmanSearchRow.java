package RGcards.SportsCardProject.dto;

import RGcards.SportsCardProject.enums.BowmanSearchType;

import java.time.LocalDateTime;

public record AdminBowmanSearchRow(
        Long id,
        BowmanSearchType searchType,
        String player,
        Short year,
        String setName,
        String username,
        LocalDateTime createdAt
) {}
