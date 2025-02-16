package com.codingforfun.repository;

import com.codingforfun.model.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    @Query("SELECT m FROM Member m LEFT JOIN FETCH m.memberDetail WHERE m.memberId = :memberId")
    Optional<Member> findWithDetails(@Param("memberId") Long memberId);
}