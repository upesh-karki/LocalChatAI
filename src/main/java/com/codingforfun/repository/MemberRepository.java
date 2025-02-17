package com.codingforfun.repository;

import com.codingforfun.model.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    @Query(nativeQuery = true, value = """
        SELECT  m.memberid, m.firstname, m.lastname, m.email, m.pass, md.occupation, md.address1, md.address2, md.city, md.country, md.zipcode, md.phon_number, md.user_name, md.profile_status
        FROM ods.member m 
        INNER JOIN ods.member_detail md ON m.memberid = md.memberid  -- Use INNER JOIN
        WHERE LOWER(m.firstname) LIKE LOWER(CONCAT('%', :name, '%')) 
           OR LOWER(m.lastname) LIKE LOWER(CONCAT('%', :name, '%'))
    """)
    Optional<Member> findByName(@Param("name") String name);

    @Query(nativeQuery = true, value = """
        SELECT  m.memberid, m.firstname, m.lastname, m.email, m.pass, md.occupation, md.address1, md.address2, md.city, md.country, md.zipcode, md.phon_number, md.user_name, md.profile_status
        FROM ods.Member m 
        INNER JOIN ods.member_detail md ON m.memberid = md.memberid
        WHERE m.memberId = :memberId
    """)
    Optional<Member> findWithDetails(@Param("memberId") Long memberId);

    // findByCustomQuery is removed.  We're not using it.
}
