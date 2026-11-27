package com.pck4x.users_service;

import com.pck4x.users_service.infrastructure.persistence.jpa.repositories.JpaUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class UsersServiceApplicationTests {

	/** El runner del admin no debe necesitar BD real en el test de contexto. */
	@MockitoBean
	private JpaUserRepository userRepository;

	@Test
	void contextLoads() {
	}

}
