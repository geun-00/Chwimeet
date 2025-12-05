package com.back.domain.review.controller;

import com.back.domain.review.dto.ReviewDto;
import com.back.domain.review.dto.ReviewSummaryDto;
import com.back.domain.review.dto.ReviewWriteReqBody;
import com.back.global.rsData.RsData;
import com.back.global.security.SecurityUser;
import com.back.standard.util.page.PagePayload;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public interface ReviewApi {
    @Operation(summary = "후기 등록", description = "반납 완료된 건에 대하여 후기를 등록합니다.")
    ResponseEntity<RsData<ReviewDto>> write(
            @PathVariable Long reservationId,
            @Valid @RequestBody ReviewWriteReqBody reqBody,
            @AuthenticationPrincipal SecurityUser securityUser
    );
    @Operation(summary = "게시글 후기 조회", description = "특정 게시글에 작성된 후기를 페이징하여 조회합니다.")
    ResponseEntity<RsData<PagePayload<ReviewDto>>> getPostReviews(
            @PathVariable Long postId,
            @ParameterObject @PageableDefault(size = 30, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    );
    @Operation(summary = "사용자 후기 조회", description = "특정 사용자에 작성된 후기를 페이징하여 조회합니다.")
    ResponseEntity<RsData<PagePayload<ReviewDto>>> getMemberReviews(
            @PathVariable Long memberId,
            @ParameterObject @PageableDefault(size = 30, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    );

    @Operation(summary = "게시글 후기 후기 통계 요약 조회 API", description = "특정 게시글의 후기 요약 통계 정보를 조회합니다.")
    public ResponseEntity<RsData<ReviewSummaryDto>> getPostReviewSummary(@PathVariable Long postId);

    @Operation(summary = "게시글 작성자 후기 통계 요약 조회 API", description = "게시글 작성자의 후기 요약 통계 정보를 조회합니다.")
    public ResponseEntity<RsData<ReviewSummaryDto>> getMemberReviewSummary(@PathVariable Long postId);

    @Operation(summary = "게시글 후기 AI 요약 API", description = "AI가 특정 게시글의 후기 목록을 요약합니다.")
    ResponseEntity<RsData<String>> summarizePostReviews(@PathVariable Long postId);

    @Operation(summary = "게시글 작성자 후기 AI 요약 API", description = "AI가 게시글 작성자의 후기 목록을 요약합니다.")
    ResponseEntity<RsData<String>> summarizeMemberReviews(@PathVariable Long memberId);
}
