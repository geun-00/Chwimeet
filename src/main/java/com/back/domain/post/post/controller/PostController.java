package com.back.domain.post.post.controller;

import com.back.domain.post.post.dto.PostCreateReqBody;
import com.back.domain.post.post.dto.PostDetailResBody;
import com.back.domain.post.post.dto.PostListResBody;
import com.back.domain.post.post.service.PostService;
import com.back.global.rsData.RsData;
import com.back.global.security.SecurityUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    // 추후 RsData<PostDto>로 변경 예정
    @PostMapping
    public ResponseEntity<RsData<Long>> createPost(
            @Valid @RequestBody PostCreateReqBody reqBody,
            @AuthenticationPrincipal SecurityUser securityUser
    ) {

        Long memberId = securityUser.getId();

        RsData<Long> body = postService.createPost(reqBody, memberId);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping
    public ResponseEntity<List<PostListResBody>> getPostList() {
        List<PostListResBody> body = postService.getPostList();
        return ResponseEntity.ok(body);
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostDetailResBody> getPostById(@PathVariable Long postId) {
        PostDetailResBody body = postService.getPostById(postId);
        return ResponseEntity.ok(body);
    }

    @GetMapping("/my")
    public ResponseEntity<List<PostListResBody>> getMyPostList(@AuthenticationPrincipal SecurityUser securityUser) {
        List<PostListResBody> body = postService.getMyPosts(securityUser.getId());
        return ResponseEntity.ok(body);
    }
}
