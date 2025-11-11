package com.back.domain.post.post.service;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.post.post.dto.PostCreateReqBody;
import com.back.domain.post.post.dto.PostListResBody;
import com.back.domain.post.post.entity.Post;
import com.back.domain.post.post.entity.PostImage;
import com.back.domain.post.post.entity.PostOption;
import com.back.domain.post.post.repository.PostRepository;
import com.back.global.exception.ServiceException;
import com.back.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final MemberRepository memberRepository;

    // TODO: 추후 구현 필요
    // private final RegionRepository regionRepository;
    // private final CategoryRepository categoryRepository;

    public RsData<Long> createPost(PostCreateReqBody reqBody, Long memberId) {

        Member author = memberRepository.findById(memberId).orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 회원입니다."));

        Post post = Post.builder()
                .title(reqBody.title())
                .content(reqBody.content())
                .receiveMethod(reqBody.receiveMethod())
                .returnMethod(reqBody.returnMethod())
                .returnAddress1(reqBody.returnAddress1())
                .returnAddress2(reqBody.returnAddress2())
                .deposit(reqBody.deposit())
                .fee(reqBody.fee())
                .author(author)
                .build();

        if (reqBody.options() != null && !reqBody.options().isEmpty()) {
            List<PostOption> postOptions = reqBody.options().stream()
                    .map(option -> PostOption.builder()
                            .post(post)
                            .name(option.name())
                            .deposit(option.deposit())
                            .fee(option.fee())
                            .build())
                    .toList();
            post.getOptions().addAll(postOptions);
        }

        if (reqBody.images() != null && !reqBody.images().isEmpty()) {
            List<PostImage> postImages = reqBody.images().stream()
                    .map(image -> PostImage.builder()
                            .post(post)
                            .imageUrl("example.com/image.jpg") // TODO: 이미지 업로드 로직 구현 후 수정
                            .isPrimary(image.isPrimary())
                            .build())
                    .toList();
            post.getImages().addAll(postImages);
        }
        postRepository.save(post);

        return RsData.success("게시글이 등록되었습니다.");
    }

    public List<PostListResBody> getPostList() {
        List<Post> posts = postRepository.findAll();

        return posts.stream()
                .map(post -> PostListResBody.builder()
                        .postId(post.getId())
                        .title(post.getTitle())
                        .thumbnailImageUrl(
                                post.getImages().stream()
                                        .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                                        .findFirst()
                                        .map(img -> img.getImageUrl())
                                        .orElse(null)
                        )
                        .categoryId(null) // 추후 카테고리 연동 필요
                        .regionIds(List.of()) // 추후 지역 연동 필요
                        .receiveMethod(post.getReceiveMethod())
                        .returnMethod(post.getReturnMethod())
                        .createdAt(post.getCreatedAt())
                        .authorNickname(post.getAuthor().getNickname())
                        .fee(post.getFee())
                        .deposit(post.getDeposit())
                        .isFavorite(false) // 추후 즐겨찾기 구현
                        .isBanned(post.getIsBanned())
                        .build()
                )
                .collect(Collectors.toList());

    }
}
