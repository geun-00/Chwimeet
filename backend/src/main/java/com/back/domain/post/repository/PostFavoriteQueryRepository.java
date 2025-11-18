package com.back.domain.post.repository;

import com.back.domain.post.entity.PostFavorite;
import com.back.global.queryDsl.CustomQuerydslRepositorySupport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import static com.back.domain.post.entity.QPost.post;
import static com.back.domain.post.entity.QPostFavorite.postFavorite;
import static com.back.domain.post.entity.QPostImage.postImage;

@Repository
public class PostFavoriteQueryRepository extends CustomQuerydslRepositorySupport {
    public PostFavoriteQueryRepository() {
        super(PostFavorite.class);
    }
    public Page<PostFavorite> findFavoritePosts(long memberId, Pageable pageable) {
        return applyPagination(
                pageable,
                contentQuery -> contentQuery
                        .selectFrom(postFavorite)
                        .join(postFavorite.post, post).fetchJoin()
                        .leftJoin(post.images, postImage).fetchJoin()
                        .where(postFavorite.member.id
                        .eq(memberId))
                        .distinct(),

                countQuery -> countQuery
                        .select(postFavorite.count())
                        .from(postFavorite)
                        .where(postFavorite.member.id.eq(memberId))


        );

    }

}
