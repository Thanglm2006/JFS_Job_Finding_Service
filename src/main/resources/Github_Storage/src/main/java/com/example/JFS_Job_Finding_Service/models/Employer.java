package com.example.JFS_Job_Finding_Service.models;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLInsert;

@Entity
@Table(name="employer")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;

    @Getter
    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private employer_type type;

    public Employer(User user, employer_type type) {
        this.user = user;
        this.type = type;
    }
    public String getFullName() {
        return user.getFullName();
    }
}
