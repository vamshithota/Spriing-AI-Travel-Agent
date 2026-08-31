package com.company.openai.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "trip_plans")
@Data
public class TripPlanEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String conversationId;
    private String destination;
    private Integer days;
    private Double budgetUSD;

    @Column(columnDefinition = "TEXT")
    private String generatedItinerary;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

}