package com.dasibom.practice.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import org.hibernate.annotations.CreationTimestamp;

@Entity
public class Diary {
    @Id
    @GeneratedValue
    @Column(name = "diaryId")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "petId")
    private Pet pet;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "authorId")
    private User author;

    @OneToMany(mappedBy = "diary", cascade = CascadeType.ALL)
    private List<DiaryImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "diary", cascade = CascadeType.ALL)
    private List<DiaryStamp> stamps;

    private String title;
    private String content;

    @CreationTimestamp
    private LocalDate createdAt;

    @CreationTimestamp
    private LocalDate updatedAt;
}
