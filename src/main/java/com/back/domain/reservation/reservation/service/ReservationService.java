package com.back.domain.reservation.reservation.service;

import com.back.domain.member.member.entity.Member;
import com.back.domain.reservation.reservation.common.ReservationStatus;
import com.back.domain.reservation.reservation.dto.CreateReservationReqBody;
import com.back.domain.reservation.reservation.entity.Reservation;
import com.back.domain.reservation.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReservationService {
    private final ReservationRepository reservationRepository;

    public Reservation create(CreateReservationReqBody reqBody, Member author) {
        // TODO: 게시글 조회
        // Post post = postService.getById(reqBody.postId());

        // 1. 기간 중복 체크
//        validateNoOverlappingReservation(
//                null, // TODO: post.getId()
//                reqBody.reservationStartAt(),
//                reqBody.reservationEndAt()
//        );

        // 2. 같은 게스트의 중복 예약 체크 (게시글 ID 필요)
        // validateNoDuplicateReservation(post.getId(), author.getId());

        Reservation reservation = Reservation.builder()
                .status(ReservationStatus.PENDING_APPROVAL)
                .receiveMethod(reqBody.receiveMethod())
                .receiveCarrier(reqBody.receiveCarrier())
                .receiveTrackingNumber(reqBody.receiveTrackingNumber())
                .receiveAddress1(reqBody.receiveAddress1())
                .receiveAddress2(reqBody.receiveAddress2())
                .returnMethod(reqBody.returnMethod())
                .returnCarrier(reqBody.returnCarrier())
                .returnTrackingNumber(reqBody.returnTrackingNumber())
                .reservationStartAt(reqBody.reservationStartAt())
                .reservationEndAt(reqBody.reservationEndAt())
                .author(author)
                .build();
        return reservationRepository.save(reservation);
    }

    // 기간 중복 체크
//    private void validateNoOverlappingReservation(
//            Long postId,
//            LocalDateTime startAt,
//            LocalDateTime endAt
//    ) {
//        boolean hasOverlap = reservationRepository.existsOverlappingReservation(
//                postId, startAt, endAt
//        );
//
//        if (hasOverlap) {
//            throw new ServiceException("400-1", "해당 기간에 이미 예약이 있습니다.");
//        }
//    }
}
