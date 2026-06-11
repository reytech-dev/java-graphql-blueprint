plugins {
	java
	id("org.springframework.boot") version "4.1.0"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "de.reytech"
version = findProperty("version")?.toString() ?: "0.1.0-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
	implementation("org.springframework.boot:spring-boot-starter-flyway")
	implementation("org.springframework.boot:spring-boot-starter-graphql")
	implementation("org.springframework.boot:spring-boot-starter-webflux")
	implementation("org.flywaydb:flyway-database-postgresql")
	implementation("org.springframework:spring-jdbc")
	runtimeOnly("io.micrometer:micrometer-registry-otlp")
	runtimeOnly("org.postgresql:postgresql")
	runtimeOnly("org.postgresql:r2dbc-postgresql")
	testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
	testImplementation("org.springframework.boot:spring-boot-starter-data-r2dbc-test")
	testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
	testImplementation("org.springframework.boot:spring-boot-starter-graphql-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webflux-test")
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testImplementation("org.testcontainers:testcontainers-junit-jupiter")
	testImplementation("org.testcontainers:testcontainers-postgresql")
	testImplementation("org.testcontainers:testcontainers-r2dbc")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}


springBoot {
    buildInfo()
}

tasks.bootJar {
    layered {
        enabled.set(true)
    }
}

tasks.bootBuildImage {
    imageName.set("ghcr.io/reytech-dev/java-graphql-blueprint:${project.version}")

    // Use a Paketo Java builder that supports Java 25.
    builder.set("paketobuildpacks/builder-noble-java-tiny:latest")

    environment.set(
        mapOf(
            "BP_JVM_VERSION" to "25",
            "BPL_JVM_HEAD_ROOM" to "10",
            "BPL_JVM_THREAD_COUNT" to "50",
            "BPL_JVM_LOADED_CLASS_COUNT" to "20000"
        )
    )

    // Enable this only when you want Gradle to push directly to the registry.
    publish.set(false)


    docker {
        publishRegistry {
            username.set(System.getenv("REGISTRY_USERNAME"))
            password.set(System.getenv("REGISTRY_PASSWORD"))
            url.set("https://ghcr.io")
        }
    }
}
