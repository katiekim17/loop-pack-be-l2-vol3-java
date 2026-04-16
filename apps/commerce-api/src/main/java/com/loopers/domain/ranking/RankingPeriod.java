package com.loopers.domain.ranking;

public enum RankingPeriod {
    DAILY, WEEKLY, MONTHLY;

    public static RankingPeriod from(String value) {
        if (value == null || value.isBlank()) {
            return DAILY;
        }
        return valueOf(value.toUpperCase());
    }
}
