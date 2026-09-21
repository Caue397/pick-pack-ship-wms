plugins {
	java
	id("org.springframework.boot") version "4.1.1"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.pickpackship"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

sourceSets {
	create("integrationTest") {
		compileClasspath += sourceSets.main.get().output + sourceSets.test.get().output
		runtimeClasspath += output + compileClasspath
	}
}

configurations.getByName("integrationTestImplementation") {
	extendsFrom(configurations.getByName("testImplementation"))
}
configurations.getByName("integrationTestRuntimeOnly") {
	extendsFrom(configurations.getByName("testRuntimeOnly"))
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-kafka")
	implementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	compileOnly("org.projectlombok:lombok")
	runtimeOnly("org.postgresql:postgresql")
	runtimeOnly("org.bouncycastle:bcprov-jdk18on:1.79")
	annotationProcessor("org.projectlombok:lombok")
	testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-kafka-test")
	testImplementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server-test")
	testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	// spring-boot-resttestclient's TestRestTemplate autoconfig needs RestTemplateBuilder,
	// which spring-boot-starter-webmvc-test doesn't pull in transitively on 4.1.1.
	testImplementation("org.springframework.boot:spring-boot-restclient")
	testCompileOnly("org.projectlombok:lombok")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testAnnotationProcessor("org.projectlombok:lombok")

	// 2.x renamed the module artifacts with a "testcontainers-" prefix
	// (org.testcontainers:postgresql -> org.testcontainers:testcontainers-postgresql).
	"integrationTestImplementation"("org.testcontainers:testcontainers-postgresql:2.0.5")
	"integrationTestImplementation"("org.testcontainers:testcontainers-kafka:2.0.5")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

val integrationTest = tasks.register<Test>("integrationTest") {
	description = "Runs integration tests (Testcontainers, requires Docker)."
	group = "verification"
	testClassesDirs = sourceSets["integrationTest"].output.classesDirs
	classpath = sourceSets["integrationTest"].runtimeClasspath
	shouldRunAfter(tasks.test)
}

tasks.check {
	dependsOn(integrationTest)
}
