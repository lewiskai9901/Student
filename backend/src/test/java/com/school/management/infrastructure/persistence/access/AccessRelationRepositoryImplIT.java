package com.school.management.infrastructure.persistence.access;

import com.school.management.domain.access.model.entity.AccessRelation;
import com.school.management.domain.access.repository.AccessRelationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link AccessRelationRepositoryImpl#deleteByResourceAndSubject}.
 *
 * <p>Regression coverage for commit {@code 1168eec3} ("fix: removeMember() now only deletes
 * the specific user's relation, not all org relations"). Before the fix, removing a single
 * member from an organization called {@code deleteByResource("org_unit", orgUnitId)}, which
 * wiped ALL access relations on that org and effectively expelled every other member too.
 * The new {@code deleteByResourceAndSubject} method narrows the DELETE to the specific
 * (resource, subject) tuple. This test verifies the SQL actually behaves that way.
 *
 * <h2>Why this test is currently disabled</h2>
 * <p>The backend test module does not yet have a Spring-context integration-test harness:
 * <ul>
 *   <li>No {@code application-test.yml} exists under {@code src/test/resources}.</li>
 *   <li>Neither H2 nor Testcontainers is declared in {@code backend/pom.xml} &mdash;
 *       the only test dependencies are {@code spring-boot-starter-test} and
 *       {@code spring-security-test}.</li>
 *   <li>The main {@code application.yml} hard-wires a MySQL Druid datasource that
 *       requires a live MySQL instance on localhost:3306 and the {@code DB_PASSWORD}
 *       environment variable. Booting the Spring context without that will fail.</li>
 *   <li>Every other test under {@code backend/src/test/java} is a pure unit test using
 *       {@code @ExtendWith(MockitoExtension.class)}; no {@code @SpringBootTest} pattern
 *       exists to copy.</li>
 * </ul>
 *
 * <p>When the integration-test harness is in place (either a Testcontainers MySQL
 * container or an H2-backed {@code application-test.yml} with MyBatis Plus dialect
 * configuration), remove {@code @Disabled} and add the following class-level annotations:
 * <pre>
 *   &#64;SpringBootTest
 *   &#64;ActiveProfiles("test")
 *   &#64;Transactional
 * </pre>
 * and inject the repository with {@code @Autowired}. The test bodies below already
 * express the expected behavior.
 *
 * <p>TODO (infra): provision integration-test infrastructure &mdash;
 * either {@code testcontainers-mysql} + Docker, or add H2 with an
 * {@code application-test.yml} profile that swaps the Druid datasource for an
 * embedded database and applies {@code database/schema/complete_schema_v2.sql}
 * via {@code spring.sql.init.schema-locations}.
 */
@Disabled("Requires Spring context + DB (see class Javadoc). Enable once integration-test harness is provisioned.")
class AccessRelationRepositoryImplIT {

    // @Autowired — uncomment once @SpringBootTest is re-enabled.
    private AccessRelationRepository repo;

    @BeforeEach
    void cleanSlate() {
        // @Transactional on the class auto-rolls-back, but leaving a hook here for
        // explicit cleanup if future tests disable per-method rollback.
    }

    /**
     * Core regression: deleting the (org_unit=100, user=1) tuple must NOT touch
     * the (org_unit=100, user=2) tuple. This is exactly the bug fixed in 1168eec3.
     */
    @Test
    void deleteByResourceAndSubject_shouldOnlyDeleteMatchingTuple() {
        AccessRelation r1 = AccessRelation.builder()
                .resourceType("org_unit").resourceId(100L)
                .relation("member").subjectType("user").subjectId(1L)
                .build();
        AccessRelation r2 = AccessRelation.builder()
                .resourceType("org_unit").resourceId(100L)
                .relation("member").subjectType("user").subjectId(2L)
                .build();
        repo.save(r1);
        repo.save(r2);

        // Only delete user 1's relation on org 100.
        repo.deleteByResourceAndSubject("org_unit", 100L, "user", 1L);

        // User 1 gone, user 2 preserved.
        assertThat(repo.exists("org_unit", 100L, "member", "user", 1L)).isFalse();
        assertThat(repo.exists("org_unit", 100L, "member", "user", 2L)).isTrue();
    }

    /**
     * Cross-resource isolation: deleting on org 100 must not affect org 200 even
     * when the same user is a member of both.
     */
    @Test
    void deleteByResourceAndSubject_shouldNotAffectOtherResources() {
        AccessRelation onOrg100 = AccessRelation.builder()
                .resourceType("org_unit").resourceId(100L)
                .relation("member").subjectType("user").subjectId(1L)
                .build();
        AccessRelation onOrg200 = AccessRelation.builder()
                .resourceType("org_unit").resourceId(200L)
                .relation("member").subjectType("user").subjectId(1L)
                .build();
        repo.save(onOrg100);
        repo.save(onOrg200);

        repo.deleteByResourceAndSubject("org_unit", 100L, "user", 1L);

        assertThat(repo.exists("org_unit", 100L, "member", "user", 1L)).isFalse();
        assertThat(repo.exists("org_unit", 200L, "member", "user", 1L)).isTrue();
    }

    /**
     * Subject-type isolation: deleting a {@code user} subject must not delete
     * an {@code org_unit} subject with the same numeric id on the same resource.
     */
    @Test
    void deleteByResourceAndSubject_shouldDistinguishSubjectType() {
        AccessRelation userSubject = AccessRelation.builder()
                .resourceType("org_unit").resourceId(100L)
                .relation("member").subjectType("user").subjectId(1L)
                .build();
        AccessRelation orgSubject = AccessRelation.builder()
                .resourceType("org_unit").resourceId(100L)
                .relation("member").subjectType("org_unit").subjectId(1L)
                .build();
        repo.save(userSubject);
        repo.save(orgSubject);

        repo.deleteByResourceAndSubject("org_unit", 100L, "user", 1L);

        assertThat(repo.exists("org_unit", 100L, "member", "user", 1L)).isFalse();
        assertThat(repo.exists("org_unit", 100L, "member", "org_unit", 1L)).isTrue();
    }
}
