package com.example.positionmanagementservice.dto;

import java.util.UUID;

public record PositionFileMetaDTO(
        UUID id,
        String name,
        String zipFileName,
        String originalFilePath,
        long loanCount,
        long paymentScheduleCount,
        long rateScheduleCount,
        long customFieldCount
) {}

