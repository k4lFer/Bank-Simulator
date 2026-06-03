package com.pck4x.users_service.application.feature.register;

import com.pck4x.sharedcontracts.event.UserCreatedEvent;
import com.pck4x.sharedcontracts.result.OutputPort;
import com.pck4x.users_service.application.dto.command.RegisterAdminCommand;
import com.pck4x.users_service.application.port.output.EventPublisher;
import com.pck4x.users_service.application.port.output.UserRepository;
import com.pck4x.users_service.domain.Role;
import com.pck4x.users_service.domain.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class RegisterAdminService implements RegisterAdminUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EventPublisher eventPublisher;
    private final String bootstrapSecret;

    public RegisterAdminService(UserRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                EventPublisher eventPublisher,
                                @Value("${app.admin-bootstrap-secret}") String bootstrapSecret) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
        this.bootstrapSecret = bootstrapSecret;
    }

    @Override
    @Transactional
    public OutputPort<UUID> execute(RegisterAdminCommand command) {
        if (command.getFirstName() == null || command.getFirstName().isBlank()) {
            return OutputPort.badRequest("First name is required");
        }
        if (command.getLastName() == null || command.getLastName().isBlank()) {
            return OutputPort.badRequest("Last name is required");
        }
        if (command.getEmail() == null || command.getEmail().isBlank()) {
            return OutputPort.badRequest("Email is required");
        }
        if (command.getPassword() == null || command.getPassword().length() < 6) {
            return OutputPort.badRequest("Password must be at least 6 characters");
        }
        if (userRepository.existsByEmail(command.getEmail())) {
            return OutputPort.conflict("Email is already in use");
        }

        User user = new User(
                UUID.randomUUID(),
                command.getFirstName(),
                command.getLastName(),
                command.getEmail(),
                passwordEncoder.encode(command.getPassword()),
                command.getPhone(),
                Role.ADMIN
        );

        user = userRepository.save(user);
        if (user == null) {
            return OutputPort.badRequest("Failed to register admin");
        }

        eventPublisher.publish("bank.user.events", new UserCreatedEvent(
                user.getId(), user.getEmail(),
                user.getFirstName() + " " + user.getLastName(),
                user.getRole().name()
        ));

        return OutputPort.created(user.getId(), "Admin registered successfully");
    }
}
