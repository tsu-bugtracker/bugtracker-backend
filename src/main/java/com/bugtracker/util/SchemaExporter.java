package com.bugtracker.util;

import jakarta.persistence.Entity;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SchemaExporter {

    private static final String DEFAULT_TARGET_FILE = "src/main/resources/db/migration/V1__init.sql";
    private static final String ENTITY_PACKAGE = "com.bugtracker.domain.entity";

    private SchemaExporter() {
    }

    public static void main(String[] args) throws IOException {
        Path targetFile = Path.of(args.length > 0 ? args[0] : DEFAULT_TARGET_FILE);

        Files.deleteIfExists(targetFile);
        if (targetFile.getParent() != null) {
            Files.createDirectories(targetFile.getParent());
        }

        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put(AvailableSettings.DIALECT, "org.hibernate.dialect.PostgreSQLDialect");
        settings.put("hibernate.boot.allow_jdbc_metadata_access", false);
        settings.put(AvailableSettings.PHYSICAL_NAMING_STRATEGY,
                CamelCaseToUnderscoresNamingStrategy.class.getName());
        settings.put(AvailableSettings.HBM2DDL_DELIMITER, ";");
        settings.put(AvailableSettings.FORMAT_SQL, true);
        settings.put(AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_ACTION, "create");
        settings.put(AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_CREATE_TARGET, targetFile.toString());
        settings.put("jakarta.persistence.schema-generation.scripts.create-source", "metadata");

        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .applySettings(settings)
                .build();
        try {
            Metadata metadata = buildMetadata(registry);
            SchemaManagementToolCoordinator.process(metadata, registry, settings, null);
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }

        System.out.println("Schema successfully generated: " + targetFile.toAbsolutePath());
    }

    private static Metadata buildMetadata(StandardServiceRegistry registry) {
        MetadataSources metadataSources = new MetadataSources(registry);

        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));

        for (BeanDefinition candidate : scanner.findCandidateComponents(ENTITY_PACKAGE)) {
            try {
                metadataSources.addAnnotatedClass(Class.forName(candidate.getBeanClassName()));
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Unable to load entity class " + candidate.getBeanClassName(), e);
            }
        }

        return metadataSources.buildMetadata();
    }
}
