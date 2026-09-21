package com.pickpackship.order;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.pickpackship.order.repository.OrderRepository;
import com.pickpackship.order.repository.OutboxEventRepository;
import com.pickpackship.order.repository.SellerRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for order-service integration tests: real Postgres + real Kafka via
 * Testcontainers, shared (singleton pattern) across every subclass in this JVM run.
 *
 * order-service never issues JWTs itself (auth-service does, from a login flow), so
 * tests mint their own tokens here with the same HS256 secret the service is
 * configured with, mirroring what auth-service would have handed the caller.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
public abstract class AbstractIntegrationTest {

    protected static final String JWT_SECRET =
            "test-only-jwt-secret-for-order-service-integration-tests-0123456789";

    protected static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));

    protected static final KafkaContainer KAFKA =
            new KafkaContainer(DockerImageName.parse("apache/kafka:3.8.0"));

    static {
        POSTGRES.start();
        KAFKA.start();
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("jwt.secret", () -> JWT_SECRET);
        // No Flyway/Liquibase yet; Spring Boot only auto-creates the schema for
        // embedded databases, and a Testcontainers Postgres doesn't count as one.
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
    }

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected OrderRepository orderRepository;

    @Autowired
    protected SellerRepository sellerRepository;

    @Autowired
    protected OutboxEventRepository outboxEventRepository;

    @BeforeEach
    void cleanDatabase() {
        outboxEventRepository.deleteAll();
        orderRepository.deleteAll();
        sellerRepository.deleteAll();
    }

    /**
     * Mints a JWT the way auth-service would for a user with these claims, signed
     * with the same secret this service's JwtDecoder is configured with in tests.
     */
    protected static String issueToken(UUID userId, String operatorId, UUID workspaceId, String role) {
        SecretKey secretKey = new SecretKeySpec(JWT_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        JwtEncoder jwtEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));

        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(userId.toString())
                .issuedAt(now)
                .expiresAt(now.plus(1, ChronoUnit.HOURS))
                .claim("operatorId", operatorId)
                .claim("workspaceId", workspaceId.toString())
                .claim("role", role)
                .build();

        return jwtEncoder
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    protected static String cookiePairFor(UUID userId, String operatorId, UUID workspaceId, String role) {
        return "access_token=" + issueToken(userId, operatorId, workspaceId, role);
    }

    protected static HttpEntity<Void> authenticatedRequest(String cookiePair) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, cookiePair);
        return new HttpEntity<>(headers);
    }

    protected static <T> HttpEntity<T> authenticatedJsonRequest(String cookiePair, T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, cookiePair);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}
