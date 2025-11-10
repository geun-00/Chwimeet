package com.back.domain.member.member.controller;

import com.back.domain.member.member.dto.MemberDto;
import com.back.domain.member.member.dto.MemberJoinReqBody;
import com.back.domain.member.member.dto.MemberLoginReqBody;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.service.AuthTokenService;
import com.back.domain.member.member.service.MemberService;
import com.back.domain.member.member.service.RefreshTokenStore;
import com.back.global.exception.ServiceException;
import com.back.global.rsData.RsData;
import com.back.global.security.SecurityUser;
import com.back.global.web.CookieHelper;
import com.back.global.web.HeaderHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;
    private final AuthTokenService authTokenService;
    private final RefreshTokenStore refreshTokenStore;
    private final CookieHelper cookieHelper;
    private final HeaderHelper headerHelper;

    @PostMapping
    public RsData<Void> join(
            @Valid @RequestBody MemberJoinReqBody reqBody
    ) {
        memberService.join(reqBody);

        return RsData.success("회원가입이 완료되었습니다.");
    }

    @PostMapping("/login")
    public RsData<MemberDto> login(
            @Valid @RequestBody MemberLoginReqBody reqBody
    ) {
        Member member = memberService.findByEmail(reqBody.email()).orElseThrow(
                () -> new ServiceException("401-1", "사용자를 찾을 수 없습니다.")
        );
        memberService.checkPassword(
                member,
                reqBody.password()
        );

        String accessToken = authTokenService.genAccessToken(member);
        String refreshToken = authTokenService.issueRefresh(member);

        cookieHelper.setCookie("accessToken", accessToken);
        headerHelper.setHeader("accessToken", accessToken);
        cookieHelper.setCookie("refreshToken", refreshToken);
        headerHelper.setHeader("refreshToken", refreshToken);

        return RsData.success(
                "%s님 환영합니다.".formatted(member.getNickname()),
                new MemberDto(member)
        );
    }

    @PostMapping("/logout")
    public RsData<Void> logout() {
        String refreshPlain = cookieHelper.getCookieValue("refreshToken", null);
        if (refreshPlain != null && !refreshPlain.isBlank()) {
            refreshTokenStore.revoke(refreshPlain);
        }

        cookieHelper.deleteCookie("accessToken");
        cookieHelper.deleteCookie("refreshToken");

        return RsData.success("로그아웃 되었습니다.");
    }

    @GetMapping("/me")
    public RsData<MemberDto> me(
            @AuthenticationPrincipal SecurityUser securityUser
    ) {
        Member member = memberService.getById(securityUser.getId());

        return RsData.success(
                "%s님 정보입니다.".formatted(member.getNickname()),
                new MemberDto(member)
        );
    }
}
