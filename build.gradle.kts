buildscript {
	repositories {
		mavenCentral()
	}
	dependencies {
		classpath("org.liquibase:liquibase-core:5.0.4")
	}
}

plugins {
	java
	id("org.springframework.boot") version "4.1.3"
	id("io.spring.dependency-management") version "1.1.7"
	id("org.liquibase.gradle") version "3.1.1"
}

group = "ru.tsu.bugtracker"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenCentral()
}

configurations {
	developmentOnly
	runtimeClasspath {
		extendsFrom(developmentOnly.get())
	}
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-websocket")
	implementation("org.springframework.boot:spring-boot-starter-mail")
	implementation("org.springframework.boot:spring-boot-starter-restclient")
	implementation("org.springframework.boot:spring-boot-starter-liquibase")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3")
	implementation(platform("software.amazon.awssdk:bom:2.59.4"))
	implementation("software.amazon.awssdk:s3")
	implementation("io.jsonwebtoken:jjwt-api:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
	runtimeOnly("org.postgresql:postgresql")
	developmentOnly("org.springframework.boot:spring-boot-docker-compose")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.security:spring-security-test")

	liquibaseRuntime(sourceSets.main.get().output)
	liquibaseRuntime("org.springframework.boot:spring-boot-starter-data-jpa")
	liquibaseRuntime("org.springframework:spring-core")
	liquibaseRuntime("org.springframework:spring-context")
	liquibaseRuntime("org.liquibase:liquibase-core:5.0.4")
	liquibaseRuntime("org.liquibase.ext:liquibase-hibernate7:5.0.4")
	liquibaseRuntime("org.postgresql:postgresql")
	liquibaseRuntime("org.apache.commons:commons-lang3")
	liquibaseRuntime("info.picocli:picocli:4.7.7")
}

liquibase {
	activities.register("main") {
		arguments = mapOf(
			"changelogFile" to "src/main/resources/db/changelog/changes/diff-generated.yaml",
			"url" to "jdbc:postgresql://localhost:5432/bugtracker",
			"username" to "bugtracker",
			"password" to "bugtracker",
			"driver" to "org.postgresql.Driver",
			"referenceUrl" to "hibernate:spring:ru.tsu.bugtracker?dialect=org.hibernate.dialect.PostgreSQLDialect",
		)
	}
	runList = "main"
}

tasks.withType<Test> {
	useJUnitPlatform()
}
