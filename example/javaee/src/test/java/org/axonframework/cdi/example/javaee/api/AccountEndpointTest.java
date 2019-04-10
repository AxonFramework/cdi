package org.axonframework.cdi.example.javaee.api;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.JerseyClientBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class AccountEndpointTest {

    private static JavaArchive axonCore() {
        return ShrinkWrap.create(JavaArchive.class, "axon-core.jar")
                .addPackages(true,
                        "org.axonframework.commandhandling",
                        "org.axonframework.common",
                        "org.axonframework.config",
                        "org.axonframework.deadline",
                        "org.axonframework.eventhandling",
                        "org.axonframework.eventsourcing",
                        "org.axonframework.messaging",
                        "org.axonframework.monitoring",
                        "org.axonframework.queryhandling",
                        "org.axonframework.serialization"
                        );
    }

    private static JavaArchive axonCdiExtension() {
        return ShrinkWrap.create(JavaArchive.class, "axon-cdi.jar")
                .addPackages(false,
                        "org.axonframework.cdi",
                        "org.axonframework.cdi.eventhandling.saga",
                        "org.axonframework.cdi.messaging.annotation",
                        "org.axonframework.cdi.stereotype",
                        "org.axonframework.cdi.transaction"
                        );
    }

    private static JavaArchive xstream() {
        return ShrinkWrap.create(JavaArchive.class, "xstream.jar")
                .addPackages(true, "com.thoughtworks.xstream");
    }

    private static JavaArchive jacksonDataTypeJsr310() {
        return ShrinkWrap.create(JavaArchive.class, "jackson-datatype-jsr310.jar")
                .addPackages(true, "com.fasterxml.jackson.datatype.jsr310");
    }

    private static JavaArchive xpp3() {
        return ShrinkWrap.create(JavaArchive.class, "xpp3.jar")
                .addPackages(true, "org.xmlpull");
    }

    @Deployment(testable=false)
    public static WebArchive javaeeAxonExample() {
        return ShrinkWrap.create(WebArchive.class, "javaee-axon-example.war")
                .addPackages(true, "org.axonframework.cdi.example.javaee")
                .addAsLibraries(axonCore(), axonCdiExtension(), jacksonDataTypeJsr310(), xstream(), xpp3())
                .addAsResource("test-persistence.xml", "META-INF/persistence.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "/META-INF/beans.xml");
    }

    @ArquillianResource
    private URI baseUrl;

    private WebTarget target;

    @Before
    public void setUp() {
        target = JerseyClientBuilder.newClient().target(baseUrl);
    }

    @Test
    public void createBasicAccount() {
        String accountId = UUID.randomUUID().toString();
        createAccount(accountId, BigDecimal.TEN);

        JsonObject accountSummary = getAccountSummary(accountId);
        assertEquals(accountId, accountSummary.getString("accountId"));
        assertEquals(BigDecimal.valueOf(0_00, 2), accountSummary.getJsonNumber("currentBalance").bigDecimalValue());
        assertEquals(BigDecimal.valueOf(10_00, 2), accountSummary.getJsonNumber("maxWithdrawAmount").bigDecimalValue());
    }

    private JsonObject getAccountSummary(String accountId) {
        return findByAccountId(getAccountSummary(), accountId).orElseThrow(() -> new AssertionError("Could not find: " + accountId));
    }

    private Optional<JsonObject> findByAccountId(JsonArray array, String accountId) {
        for (JsonObject accountSummary: array.getValuesAs(JsonObject.class)) {
            if (accountId.equals(accountSummary.getString("accountId"))) {
                return Optional.of(accountSummary);
            }
        }
        return Optional.empty();
    }

    private void createAccount(String accountId, BigDecimal overdrawftLimit) {
        Response response = target.path("api/account").request()
            .accept(MediaType.APPLICATION_JSON)
            .post(Entity.entity(Json.createObjectBuilder()
                    .add("accountId", accountId)
                    .add("overdraftLimit", overdrawftLimit)
                    .build().toString(), MediaType.APPLICATION_JSON));
        assertEquals(200, response.getStatus());
    }

    private JsonArray getAccountSummary() {
        Response response = target.path("api/account").request()
                .accept(MediaType.APPLICATION_JSON)
                .get();

        assertEquals(200, response.getStatus());
        return Json.createReader(response.readEntity(InputStream.class)).readArray();
    }
}
