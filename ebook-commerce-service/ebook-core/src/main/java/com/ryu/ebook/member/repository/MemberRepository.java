package com.ryu.ebook.member.repository;

import com.ryu.ebook.member.domain.Member;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {

    boolean existsByLoginId(String loginId);

    boolean existsByEmail(String email);

    Optional<Member> findByLoginId(String loginId);

    @Query(value = """
            select m from Member m
            where :keyword is null or m.loginId like concat('%', :keyword, '%') or m.name like concat('%', :keyword, '%')
               or m.email like concat('%', :keyword, '%')
            """,
            countQuery = """
            select count(m) from Member m
            where :keyword is null or m.loginId like concat('%', :keyword, '%') or m.name like concat('%', :keyword, '%')
               or m.email like concat('%', :keyword, '%')
            """)
    Page<Member> search(@Param("keyword") String keyword, Pageable pageable);
}
