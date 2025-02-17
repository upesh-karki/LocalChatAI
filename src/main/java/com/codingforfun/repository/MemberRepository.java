package com.codingforfun.repository;

import com.codingforfun.model.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    @Query("""
        SELECT m FROM Member m 
        JOIN FETCH m.memberDetail md 
        WHERE LOWER(m.firstName) LIKE LOWER(CONCAT('%', :name, '%')) 
           OR LOWER(m.lastName) LIKE LOWER(CONCAT('%', :name, '%'))
    """)
    Optional<Member> findByName(@Param("name") String name);

    @Query(value = """
        SELECT m FROM Member m 
        JOIN FETCH m.memberDetail md 
        WHERE m.memberId = :memberId
    """)
    Optional<Member> findWithDetails(@Param("memberId") Long memberId);
}
