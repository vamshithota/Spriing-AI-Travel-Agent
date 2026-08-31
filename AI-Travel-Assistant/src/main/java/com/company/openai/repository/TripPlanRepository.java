package com.company.openai.repository;

import com.company.openai.entity.TripPlanEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TripPlanRepository extends JpaRepository<TripPlanEntity, Long> {
    List<TripPlanEntity> findByConversationId(String conversationId);
}