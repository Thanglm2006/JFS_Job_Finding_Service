package com.example.JFS_Job_Finding_Service.models;

import com.example.JFS_Job_Finding_Service.models.Enum.EmployerType;
import com.example.JFS_Job_Finding_Service.models.Enum.VerificationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter @Setter
@Table(name = "employer")
@NoArgsConstructor
@AllArgsConstructor
public class Employer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;

    @Column(name = "org_name", nullable = false)
    private String orgName;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private EmployerType type;

    @Column(name = "custom_type")
    private String customType;

    @Column(name = "tax_code", unique = true)
    private String taxCode;

    @Column(name = "business_license_url")
    private String businessLicenseUrl;

    @Column(name = "business_code")
    private String businessCode;

    @Column(name = "company_website")
    private String companyWebsite;

    @Column(name = "company_email")
    private String companyEmail;

    @Column(name = "headquarters_address")
    private String headquartersAddress;

    @Column(name = "id_card_number")
    private String idCardNumber;

    @Column(name = "id_card_front")
    private String idCardFront;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "varchar(255) default 'PENDING'")
    private VerificationStatus status = VerificationStatus.PENDING;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;


    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}