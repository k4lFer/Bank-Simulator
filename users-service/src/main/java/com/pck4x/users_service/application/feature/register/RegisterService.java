package com.pck4x.users_service.application.feature.register;

import com.pck4x.sharedcontracts.event.UserCreatedEvent;
import com.pck4x.sharedcontracts.result.OutputPort;
import com.pck4x.users_service.application.dto.command.RegisterUserCommand;
import com.pck4x.users_service.application.port.output.EventPublisher;
import com.pck4x.users_service.application.port.output.UserRepository;
import com.pck4x.users_service.domain.Role;
import com.pck4x.users_service.domain.User;
import com.pck4x.users_service.domain.UserProfile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class RegisterService implements RegisterUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EventPublisher eventPublisher;
    private final RegisterValidator validator;

    public RegisterService(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           EventPublisher eventPublisher,
                           RegisterValidator validator) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
        this.validator = validator;
    }

    @Override
    @Transactional
    public OutputPort<UUID> execute(RegisterUserCommand command) {
        if (!validator.validate(command)) {
            return OutputPort.failures(validator.getHttpStatusCode(), validator.getErrors());
        }

        User user = new User(
                UUID.randomUUID(),
                command.getFirstName(),
                command.getLastName(),
                command.getEmail(),
                passwordEncoder.encode(command.getPassword()),
                command.getPhone(),
                Role.CUSTOMER
        );

        if (command.getDateOfBirth() != null || command.getAddress() != null
                || command.getIdDocument() != null || command.getOccupation() != null) {
            UserProfile profile = new UserProfile(
                    command.getDateOfBirth(),
                    command.getAddress(),
                    command.getIdDocument(),
                    command.getOccupation()
            );
            user.setProfile(profile);
        }

        user = userRepository.save(user);

        if (user == null) {
            return OutputPort.badRequest("Failed to register user");
        }

        eventPublisher.publish("bank.user.events", new UserCreatedEvent(
                user.getId(), user.getEmail(),
                user.getFirstName() + " " + user.getLastName(),
                user.getRole().name()
        ));

        return OutputPort.created(user.getId(),
                "User registered successfully");
    }
}
