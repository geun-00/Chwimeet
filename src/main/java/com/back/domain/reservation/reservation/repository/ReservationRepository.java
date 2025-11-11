package com.back.domain.reservation.reservation.repository;

import com.back.domain.reservation.reservation.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    // TODO: QueryDSL로 변경 예정
//    @Query("""
//        SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
//        FROM Reservation r
//        WHERE r.post.id = :postId
//        AND r.status NOT IN ('CANCELLED', 'REJECTED')
//        AND (r.reservationStartAt < :endAt AND r.reservationEndAt > :startAt)
//        """)
//    boolean existsOverlappingReservation(@Param("postId") Long postId,
//                                         @Param("startAt") LocalDateTime startAt,
//                                         @Param("endAt") LocalDateTime endAt);
}
