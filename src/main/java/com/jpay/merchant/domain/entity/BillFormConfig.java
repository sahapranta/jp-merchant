package com.jpay.merchant.domain.entity;

import jakarta.persistence.*;
import lombok.*;
// ──────────────────────────────────────────────────
// BillFormConfig — dynamic form schema for student portal
// ──────────────────────────────────────────────────
@Entity @Table(name = "bill_form_config")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BillFormConfig {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id", nullable = false)
    private Bill bill;

    @Column(name = "form_schema", columnDefinition = "CLOB", nullable = false)
    private String formSchema;
    /*
        JSON array example:
        [
          {"fieldCode":"STUDENT_NAME","fieldType":"TEXT","label":"Full Name","required":true},
          {"fieldCode":"ROLL_NO","fieldType":"TEXT","label":"Roll No","required":true},
          {"fieldCode":"GROUP","fieldType":"SELECT","label":"Group","required":true,
           "options":["Science","Commerce","Arts"]},
          {"fieldCode":"MOBILE","fieldType":"PHONE","label":"Mobile Number","required":false}
        ]
    */

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private java.time.LocalDateTime createdAt = java.time.LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private java.time.LocalDateTime updatedAt = java.time.LocalDateTime.now();
}
