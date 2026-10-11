package com.project.app;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import com.project.app.support.Slice0DatabaseGuard;
import com.project.app.supplier.model.Supplier;
import com.project.app.userAccount.model.User;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("slice0-test")
@ContextConfiguration(initializers = Slice0DatabaseGuard.class)
class AppApplicationTests {

    @Autowired EntityManagerFactory entityManagerFactory;
    @Autowired JdbcTemplate jdbc;

	@Test
	void contextLoads() {
		assertNotNull(entityManagerFactory.getMetamodel().entity(Supplier.class));
		assertThrows(IllegalArgumentException.class, () -> entityManagerFactory.getMetamodel().entity(User.class));
		assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE version='1' AND success", Integer.class));
		assertEquals(java.util.List.of("flyway_schema_history", "suppliers"), jdbc.queryForList(
				"SELECT table_name FROM information_schema.tables WHERE table_schema='public' ORDER BY table_name", String.class));
	}

}
