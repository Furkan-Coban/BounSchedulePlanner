package com.furkan.scheduler.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "departments", uniqueConstraints =@UniqueConstraint(columnNames = "code"))
public class Department {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String code; //Short-names of the courses e.g CMPE

    @Column(nullable = false)
    private String name; //Full names of the courses e.g. Computer Engineering
}
