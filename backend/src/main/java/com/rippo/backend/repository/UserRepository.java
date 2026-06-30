package com.rippo.backend.repository;

import com.rippo.backend.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByGithubId(Long githubId);
}
