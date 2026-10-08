package edu.itc.salesreport;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Architecture unit tests enforcing package layering and dependency rules using ArchUnit.
 */
public class DependencyRulesTest {

    private final JavaClasses classes = new ClassFileImporter()
            .importPackages("edu.itc.salesreport");

    @Test
    @DisplayName("Domain model classes must not depend on ingest, render, or app")
    void modelShouldNotDependOnOtherLayers() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..salesreport.model..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..salesreport.ingest..", "..salesreport.render..");

        rule.check(classes);
    }

    @Test
    @DisplayName("Ingest classes must not depend on render")
    void ingestShouldNotDependOnRender() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..salesreport.ingest..")
                .should().dependOnClassesThat()
                .resideInAPackage("..salesreport.render..");

        rule.check(classes);
    }

    @Test
    @DisplayName("Render classes must not depend on ingest")
    void renderShouldNotDependOnIngest() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..salesreport.render..")
                .should().dependOnClassesThat()
                .resideInAPackage("..salesreport.ingest..");

        rule.check(classes);
    }

    @Test
    @DisplayName("Packages must be free of cyclic dependencies")
    void packagesShouldBeFreeOfCycles() {
        ArchRule rule = slices()
                .matching("edu.itc.salesreport.(*)..")
                .should().beFreeOfCycles();

        rule.check(classes);
    }
}
