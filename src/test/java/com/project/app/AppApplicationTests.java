package com.project.app;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import com.project.app.support.Slice0DatabaseGuard;
import com.project.app.identity.model.Account;
import com.project.app.identity.model.IdentityToken;
import com.project.app.identity.model.Organization;
import com.project.app.marketplace.model.Offering;
import com.project.app.marketplace.model.SupplierProfile;
import com.project.app.procurement.model.Rfq;
import com.project.app.procurement.model.RfqInvitation;
import com.project.app.procurement.model.RfqItem;
import com.project.app.procurement.model.SupplierContact;
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
		assertNotNull(entityManagerFactory.getMetamodel().entity(Account.class));
		assertNotNull(entityManagerFactory.getMetamodel().entity(Organization.class));
		assertNotNull(entityManagerFactory.getMetamodel().entity(IdentityToken.class));
		assertNotNull(entityManagerFactory.getMetamodel().entity(SupplierProfile.class));
		assertNotNull(entityManagerFactory.getMetamodel().entity(Offering.class));
		assertNotNull(entityManagerFactory.getMetamodel().entity(SupplierContact.class));
		assertNotNull(entityManagerFactory.getMetamodel().entity(Rfq.class));
		assertNotNull(entityManagerFactory.getMetamodel().entity(RfqItem.class));
		assertNotNull(entityManagerFactory.getMetamodel().entity(RfqInvitation.class));
		assertNotNull(entityManagerFactory.getMetamodel().entity(com.project.app.procurement.model.Quotation.class));
		assertNotNull(entityManagerFactory.getMetamodel().entity(com.project.app.procurement.model.QuotationItem.class));
        assertNotNull(entityManagerFactory.getMetamodel().entity(com.project.app.procurement.model.PurchaseOrder.class));
        assertNotNull(entityManagerFactory.getMetamodel().entity(com.project.app.procurement.model.PurchaseOrderItem.class));
        assertNotNull(entityManagerFactory.getMetamodel().entity(com.project.app.procurement.model.OrderEvent.class));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE version='8' AND success",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE version='7' AND success",Integer.class));
		assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE version='6' AND success", Integer.class));
		assertThrows(IllegalArgumentException.class, () -> entityManagerFactory.getMetamodel().entity(User.class));
		assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE version='1' AND success", Integer.class));
		assertEquals(5, jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE version IN ('1','2','3','4','5') AND success", Integer.class));
		// REQ-S2-005: the legacy `suppliers` table (V1) is retained unused, with no entity mapping to it anymore.
		assertEquals(java.util.List.of("accounts", "demo_seed_completions", "flyway_schema_history", "identity_tokens", "marketplace_offerings",
				"marketplace_profiles", "order_events", "organizations", "purchase_order_items", "purchase_orders", "quotation_items", "quotations", "rfq_invitations", "rfq_items", "rfqs", "supplier_contacts",
				"suppliers"), jdbc.queryForList(
				"SELECT table_name FROM information_schema.tables WHERE table_schema='public' ORDER BY table_name", String.class));
	}

}
