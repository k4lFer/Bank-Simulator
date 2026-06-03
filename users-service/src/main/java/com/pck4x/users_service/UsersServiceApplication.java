package com.pck4x.users_service;

import com.pck4x.users_service.domain.Role;
import com.pck4x.users_service.domain.User;
import com.pck4x.users_service.infrastructure.persistence.jpa.entities.UserEntity;
import com.pck4x.users_service.infrastructure.persistence.jpa.repositories.JpaUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.UUID;

@SpringBootApplication
public class UsersServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(UsersServiceApplication.class, args);
	}

	CommandLineRunner initAdmin(JpaUserRepository userRepo, PasswordEncoder encoder) {
		return args -> {
			if (!userRepo.existsByRole("ADMIN")) {
				UserEntity admin = new UserEntity();
				admin.setId(UUID.randomUUID());
				admin.setFirstName("Admin");
				admin.setLastName("User");
				admin.setEmail("admin@bank.com");
				admin.setPassword(encoder.encode("admin123"));
				admin.setRole(Role.ADMIN);
				admin.setActive(true);
				admin.setCreatedAt(LocalDateTime.now());
				admin.setUpdatedAt(LocalDateTime.now());
				userRepo.save(admin);
				System.out.println("✅ Default admin created: admin@bank.com / admin123");
			}
		};
	}
}
