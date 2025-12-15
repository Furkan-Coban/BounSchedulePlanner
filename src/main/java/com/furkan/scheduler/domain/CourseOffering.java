package com.furkan.scheduler.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;


@Entity
@Data
@NoArgsConstructor @AllArgsConstructor
@Table(name = "course_offerings",
        uniqueConstraints = @UniqueConstraint(columnNames={"term_id","courseCodeSec"}))
public class CourseOffering {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Term term;
    @OneToMany(mappedBy = "offering",cascade = CascadeType.ALL,orphanRemoval = true)
    private List<Meeting> meetings = new ArrayList<>();
    @ManyToOne(optional = false)
    private Department department;
    @Column(nullable = false)
    private String courseCodeSec;
    @Column(nullable = false)
    private String courseName;
    @Column(nullable = false)
    private int credits;
    @Column(nullable = false)
    private int ects;

    private String instructor;   // nullable
}
