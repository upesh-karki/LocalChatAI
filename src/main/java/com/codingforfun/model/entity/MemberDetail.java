package com.codingforfun.model.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "member_detail", schema = "ods")
public class MemberDetail {
    @Id
    @Column(name = "memberid")
    private Long memberId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "memberid")
    private Member member;

    private String occupation;
    @Column(columnDefinition = "CLOB")
    private String address1;
    @Column(columnDefinition = "CLOB")
    private String address2;
    @Column(columnDefinition = "CLOB")
    private String city;
    @Column(columnDefinition = "CLOB")
    private String country;
    private Integer zipcode;

    @Column(name = "phon_number")
    private String phoneNumber;

    @Column(name = "user_name", columnDefinition = "CLOB")
    private String userName;

    @Column(name = "profile_status")
    private String profileStatus;
}