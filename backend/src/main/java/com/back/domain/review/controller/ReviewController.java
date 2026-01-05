package com.back.domain.review.controller;

import com.back.domain.review.dto.ReviewDto;
import com.back.domain.review.dto.ReviewSummaryDto;
import com.back.domain.review.dto.ReviewWriteReqBody;
import com.back.domain.review.service.ReviewService;
import com.back.domain.review.service.ReviewSummaryService;
import com.back.global.rsData.RsData;
import com.back.global.security.SecurityUser;
import com.back.standard.util.page.PagePayload;
import com.back.standard.util.page.PageUt;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReviewController implements ReviewApi {
    private final ReviewService reviewService;
    private final ReviewSummaryService reviewSummaryService;

    @PostMapping("/reviews/{reservationId}")
    public ResponseEntity<RsData<ReviewDto>> write(
            @PathVariable Long reservationId,
            @Valid @RequestBody ReviewWriteReqBody reqBody,
            @AuthenticationPrincipal SecurityUser securityUser
    ) {
        ReviewDto review  = reviewService.writeReview(reservationId, reqBody, securityUser.getId());
        RsData<ReviewDto> body = new RsData<>(HttpStatus.CREATED, "리뷰가 작성되었습니다.", review);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping("/posts/{postId}/reviews")
    public ResponseEntity<RsData<PagePayload<ReviewDto>>> getPostReviews(
            @PathVariable Long postId,
            @ParameterObject @PageableDefault(size = 30, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ){
        Page<ReviewDto> pages = reviewService.getPostReviews(pageable, postId);
        RsData<PagePayload<ReviewDto>> body = new RsData<>(HttpStatus.OK, "성공", PageUt.of(pages));
        return ResponseEntity.ok(body);
    }

    @GetMapping("/members/{memberId}/reviews")
    public ResponseEntity<RsData<PagePayload<ReviewDto>>> getMemberReviews(
            @PathVariable Long memberId,
            @ParameterObject @PageableDefault(size = 30, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ){
        Page<ReviewDto> pages = reviewService.getMemberReviews(pageable, memberId);
        RsData<PagePayload<ReviewDto>> body = new RsData<>(HttpStatus.OK, "성공", PageUt.of(pages));
        return ResponseEntity.ok(body);
    }

    @GetMapping("/posts/{postId}/reviews/summary")
    public ResponseEntity<RsData<ReviewSummaryDto>> getPostReviewSummary(
            @PathVariable Long postId
    ){
        ReviewSummaryDto reviewSummaryDto = reviewService.findPostReceivedReviewSummary2(postId);
        RsData<ReviewSummaryDto> body = new RsData<>(HttpStatus.OK, "성공", reviewSummaryDto);
        return ResponseEntity.ok(body);
    }

    @GetMapping("/members/{memberId}/reviews/summary")
    public ResponseEntity<RsData<ReviewSummaryDto>> getMemberReviewSummary(
            @PathVariable Long memberId
    ){
        ReviewSummaryDto reviewSummaryDto = reviewService.findMemberReceivedReviewSummary2(memberId);
        RsData<ReviewSummaryDto> body = new RsData<>(HttpStatus.OK, "성공", reviewSummaryDto);
        return ResponseEntity.ok(body);
    }

    @GetMapping("/posts/{id}/reviews/summary/ai")
    public ResponseEntity<RsData<String>> summarizePostReviews(@PathVariable Long id) {
        String body = reviewSummaryService.summarizePostReviews(id);

        return ResponseEntity.ok(new RsData<>(HttpStatus.OK, HttpStatus.OK.name(), body));
    }

    @GetMapping("/members/{id}/reviews/summary/ai")
    public ResponseEntity<RsData<String>> summarizeMemberReviews(@PathVariable Long id) {
        String body = reviewSummaryService.summarizeMemberReviews(id);

        return ResponseEntity.ok(new RsData<>(HttpStatus.OK, HttpStatus.OK.name(), body));
    }
}
