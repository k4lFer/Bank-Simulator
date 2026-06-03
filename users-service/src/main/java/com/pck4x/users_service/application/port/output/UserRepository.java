package com.pck4x.users_service.application.port.output;

import com.pck4x.users_service.domain.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    User save(User user);
    Optional<User> findByEmail(String email);
    Optional<User> findById(UUID id);
    List<User> findAll();
    boolean existsByEmail(String email);
}
