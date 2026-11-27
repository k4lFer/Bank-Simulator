package com.pck4x.users_service;

import com.pck4x.users_service.domain.Role;
import com.pck4x.users_service.infrastructure.persistence.jpa.entities.UserEntity;
import com.pck4x.users_service.infrastructure.persistence.jpa.repositories.JpaUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Fase 0.7: el admin de demo se crea al arrancar solo si no existe. */
class AdminBootstrapTest {

    private JpaUserRepository repository;
    private PasswordEncoder encoder;
    private UsersServiceApplication application;

    @BeforeEach
    void setUp() {
        repository = mock(JpaUserRepository.class);
        encoder = mock(PasswordEncoder.class);
        application = new UsersServiceApplication();
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(encoder.encode("admin123")).thenReturn("encoded-admin123");
    }

    @Test
    void creaAdminConPasswordCodificadoCuandoNoHayNinguno() throws Exception {
        when(repository.existsByRole(Role.ADMIN)).thenReturn(false);

        application.initAdmin(repository, encoder).run();

        var captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(repository).save(captor.capture());
        var admin = captor.getValue();
        assertEquals("admin@bank.com", admin.getEmail());
        assertEquals(Role.ADMIN, admin.getRole());
        assertEquals("encoded-admin123", admin.getPassword());
        assertTrue(admin.isActive());
        assertNotNull(admin.getId());
    }

    @Test
    void noCreaAdminSiYaExisteUno() throws Exception {
        when(repository.existsByRole(Role.ADMIN)).thenReturn(true);

        application.initAdmin(repository, encoder).run();

        verify(repository, never()).save(any());
        verifyNoInteractions(encoder);
    }
}
