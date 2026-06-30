package com.rippo.backend.repository;

import com.rippo.backend.entity.ChatSession;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    List<ChatSession> findByUserIdOrderByUpdatedAtDesc(Long userId);
}
