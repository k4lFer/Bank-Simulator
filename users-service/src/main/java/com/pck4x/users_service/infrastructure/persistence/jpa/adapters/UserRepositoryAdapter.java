package com.pck4x.users_service.infrastructure.persistence.jpa.adapters;

import com.pck4x.users_service.application.port.output.UserRepository;
import com.pck4x.users_service.domain.User;
import com.pck4x.users_service.infrastructure.persistence.jpa.repositories.JpaUserRepository;
import com.pck4x.users_service.infrastructure.persistence.mapper.UserPersistenceMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class UserRepositoryAdapter implements UserRepository {

    private final JpaUserRepository jpaUserRepository;

    public UserRepositoryAdapter(JpaUserRepository jpaUserRepository) {
        this.jpaUserRepository = jpaUserRepository;
    }

    @Override
    public User save(User user) {
        var entity = UserPersistenceMapper.INSTANCE.toEntity(user);
        entity = jpaUserRepository.save(entity);
        return UserPersistenceMapper.INSTANCE.toDomain(entity);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaUserRepository.findByEmail(email)
                .map(UserPersistenceMapper.INSTANCE::toDomain);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpaUserRepository.findById(id)
                .map(UserPersistenceMapper.INSTANCE::toDomain);
    }

    @Override
    public List<User> findAll() {
        return jpaUserRepository.findAll().stream()
                .map(UserPersistenceMapper.INSTANCE::toDomain)
                .toList();
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaUserRepository.existsByEmail(email);
    }
}
