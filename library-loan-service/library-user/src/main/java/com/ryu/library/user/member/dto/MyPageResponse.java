package com.ryu.library.user.member.dto;

import java.time.LocalDate;

public record MyPageResponse(String loginId, String name, String email, String phone, boolean suspended,
        LocalDate suspendedUntil) {
}
