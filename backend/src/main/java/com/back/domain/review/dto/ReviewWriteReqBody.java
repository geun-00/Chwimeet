package com.back.domain.review.dto;

import jakarta.validation.constraints.*;

public record ReviewWriteReqBody(
        @NotNull
        @Min(1)
        @Max(5)
        int equipmentScore,

        @NotNull
        @Min(1)
        @Max(5)
        int kindnessScore,

        @NotNull
        @Min(1)
        @Max(5)
        int responseTimeScore,

        @NotBlank
        @Size(max = 255, message = "후기는 최대 255자까지 입력 가능합니다.")
        String comment
) {
}
