package com.pck4x.users_service.infrastructure.persistence.mapper;

import com.pck4x.users_service.domain.User;
import com.pck4x.users_service.domain.UserProfile;
import com.pck4x.users_service.infrastructure.persistence.jpa.entities.UserEntity;

import java.time.LocalDateTime;

public class UserPersistenceMapper {

    public static final UserPersistenceMapper INSTANCE = new UserPersistenceMapper();

    private UserPersistenceMapper() {}

    public UserEntity toEntity(User user) {
        UserEntity entity = new UserEntity();
        entity.setId(user.getId());
        entity.setFirstName(user.getFirstName());
        entity.setLastName(user.getLastName());
        entity.setEmail(user.getEmail());
        entity.setPassword(user.getPassword());
        entity.setPhone(user.getPhone());
        entity.setRole(user.getRole());
        entity.setActive(user.isActive());
        entity.setCreatedAt(user.getCreatedAt() != null ? user.getCreatedAt() : LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        if (user.getProfile() != null) {
            var profileEntity = new com.pck4x.users_service.infrastructure.persistence.jpa.entities.UserProfile();
            profileEntity.setUser(entity);
            profileEntity.setDateOfBirth(user.getProfile().getDateOfBirth());
            profileEntity.setAddress(user.getProfile().getAddress());
            profileEntity.setIdDocument(user.getProfile().getIdDocument());
            profileEntity.setOccupation(user.getProfile().getOccupation());
            profileEntity.setCreatedAt(user.getProfile().getCreatedAt() != null
                    ? user.getProfile().getCreatedAt() : LocalDateTime.now());
            entity.setProfile(profileEntity);
        }

        return entity;
    }

    public User toDomain(UserEntity entity) {
        User user = new User(
                entity.getId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getEmail(),
                entity.getPassword(),
                entity.getPhone(),
                entity.getRole()
        );
        user.setActive(entity.isActive());
        user.setCreatedAt(entity.getCreatedAt());
        user.setUpdatedAt(entity.getUpdatedAt());

        if (entity.getProfile() != null) {
            UserProfile profile = new UserProfile();
            profile.setDateOfBirth(entity.getProfile().getDateOfBirth());
            profile.setAddress(entity.getProfile().getAddress());
            profile.setIdDocument(entity.getProfile().getIdDocument());
            profile.setOccupation(entity.getProfile().getOccupation());
            profile.setCreatedAt(entity.getProfile().getCreatedAt());
            user.setProfile(profile);
        }

        return user;
    }
}
