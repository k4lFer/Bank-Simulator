package com.pck4x.users_service.application.feature.register;

import com.pck4x.sharedcontracts.interfaces.IInputValidator;
import com.pck4x.users_service.application.dto.command.RegisterUserCommand;
import com.pck4x.users_service.application.port.output.UserRepository;

import lombok.AllArgsConstructor;

import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class RegisterValidator extends IInputValidator<RegisterUserCommand> {
    private final UserRepository userRepository;

    @Override
    public boolean validate(RegisterUserCommand input) {
        clearErrors();

        if (input.getFirstName() == null || input.getFirstName().isBlank()) {
            this.addError("ERROR", "First name is required");
        }
        if (input.getLastName() == null || input.getLastName().isBlank()) {
            this.addError("ERROR", "Last name is required");
        }
        if (input.getEmail() == null || input.getEmail().isBlank()) {
            this.addError("ERROR", "Email is required");
        } else if (!input.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            this.addError("ERROR", "Email format is invalid");
        }
        if (input.getPassword() == null || input.getPassword().isBlank()) {
            this.addError("ERROR", "Password is required");
        } else if (input.getPassword().length() < 6) {
            this.addError("ERROR", "Password must be at least 6 characters long");
        }

        if (this.hasErrors()) {
            this.httpStatus = HttpStatusCode.valueOf(422);
            return false;
        }

        if (userRepository.existsByEmail(input.getEmail())) {
            this.addError("ERROR", "Email is already in use");
            this.httpStatus = HttpStatusCode.valueOf(409);
            return false;
        }
        
        return true;
    }
}
