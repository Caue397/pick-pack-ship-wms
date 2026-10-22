package com.pickpackship.stock;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.pickpackship.stock.repository.ProductRepository;
import com.pickpackship.stock.repository.StockRepository;
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
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for stock-service integration tests: real Postgres via Testcontainers, shared
 * (singleton pattern) across every subclass in this JVM run. Kafka is not started yet because
 * stock-service has no producer or consumer; add it with the first event flow.
 *
 * stock-service never issues JWTs itself (auth-service does), so tests mint their own tokens
 * here with the same HS256 secret the service is configured with.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
public abstract class AbstractIntegrationTest {

    protected static final String JWT_SECRET =
            "test-only-jwt-secret-for-stock-service-integration-tests-0123456789";

    protected static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("jwt.secret", () -> JWT_SECRET);
        // No Flyway/Liquibase yet; Spring Boot only auto-creates the schema for
        // embedded databases, and a Testcontainers Postgres doesn't count as one.
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
    }

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected ProductRepository productRepository;

    @Autowired
    protected StockRepository stockRepository;

    @BeforeEach
    void cleanDatabase() {
        stockRepository.deleteAll();
        productRepository.deleteAll();
    }

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
}
