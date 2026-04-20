package com.school.management.infrastructure.access;

import com.school.management.infrastructure.extension.DataScopePlugin.DataScopeResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PluginDataScopeRouter}.
 *
 * Covers:
 *  - dim exists + resolver bean wired → delegates to resolver
 *  - dim not found in DB → null (caller degrades to SELF)
 *  - bean resolution failure (ClassNotFoundException) → null
 *  - resolver throws at runtime → null
 *  - cache: same dimCode lookups hit JDBC once
 *  - empty resolver_type → null
 */
class PluginDataScopeRouterTest {

    private JdbcTemplate jdbc;
    private ApplicationContext ctx;
    private PluginDataScopeRouter router;

    @BeforeEach
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        ctx = mock(ApplicationContext.class);
        router = new PluginDataScopeRouter(jdbc, ctx);
    }

    @Test
    void resolve_happyPath_returnsResolverResult() {
        DataScopeResolver resolver = mock(DataScopeResolver.class);
        when(resolver.resolve(eq(42L), eq("student"))).thenReturn(List.of(1L, 2L, 3L));
        FakeResolver wrapper = new FakeResolver(resolver);

        when(jdbc.queryForObject(any(String.class), eq(String.class), any(Object[].class)))
                .thenReturn(FakeResolver.class.getName());
        when(ctx.getBean(FakeResolver.class)).thenReturn(wrapper);

        List<Long> ids = router.resolve("BY_MAJOR", 42L, "student");

        assertThat(ids).containsExactly(1L, 2L, 3L);
        verify(resolver).resolve(42L, "student");
    }

    @Test
    void resolve_dimNotFoundInDb_returnsNull() {
        when(jdbc.queryForObject(any(String.class), eq(String.class), any(Object[].class)))
                .thenThrow(new EmptyResultDataAccessException(1));

        List<Long> ids = router.resolve("UNKNOWN_DIM", 1L, "student");

        assertThat(ids).isNull();
    }

    @Test
    void resolve_resolverClassNotFound_returnsNull() {
        when(jdbc.queryForObject(any(String.class), eq(String.class), any(Object[].class)))
                .thenReturn("com.example.DoesNotExist");

        List<Long> ids = router.resolve("BY_GRADE", 1L, "student");

        assertThat(ids).isNull();
    }

    @Test
    void resolve_emptyResolverType_returnsNull() {
        when(jdbc.queryForObject(any(String.class), eq(String.class), any(Object[].class)))
                .thenReturn("");

        List<Long> ids = router.resolve("BY_EMPTY", 1L, "student");

        assertThat(ids).isNull();
    }

    @Test
    void resolve_resolverThrows_returnsNull() {
        DataScopeResolver throwing = (uid, rt) -> { throw new RuntimeException("boom"); };
        FakeResolver wrapper = new FakeResolver(throwing);

        when(jdbc.queryForObject(any(String.class), eq(String.class), any(Object[].class)))
                .thenReturn(FakeResolver.class.getName());
        when(ctx.getBean(FakeResolver.class)).thenReturn(wrapper);

        List<Long> ids = router.resolve("BY_THROW", 1L, "student");

        assertThat(ids).isNull();
    }

    @Test
    void resolve_cachedAfterFirstLookup_jdbcHitOnlyOnce() {
        DataScopeResolver resolver = mock(DataScopeResolver.class);
        when(resolver.resolve(any(), any())).thenReturn(List.of(7L));
        FakeResolver wrapper = new FakeResolver(resolver);

        when(jdbc.queryForObject(any(String.class), eq(String.class), any(Object[].class)))
                .thenReturn(FakeResolver.class.getName());
        when(ctx.getBean(FakeResolver.class)).thenReturn(wrapper);

        router.resolve("BY_MAJOR", 1L, "student");
        router.resolve("BY_MAJOR", 2L, "teacher");
        router.resolve("BY_MAJOR", 3L, "place");

        // JDBC should be queried exactly ONCE for the dim_code → resolver_type mapping
        verify(jdbc, times(1)).queryForObject(any(String.class), eq(String.class), any(Object[].class));
        // But resolver.resolve should be called 3 times (once per resolve invocation)
        verify(resolver, times(3)).resolve(any(), any());
    }

    @Test
    void clearCache_forcesReload() {
        when(jdbc.queryForObject(any(String.class), eq(String.class), any(Object[].class)))
                .thenReturn("com.example.DoesNotExist");

        router.resolve("BY_MAJOR", 1L, "student");
        router.clearCache();
        router.resolve("BY_MAJOR", 1L, "student");

        // After clearCache, JDBC is hit again
        verify(jdbc, times(2)).queryForObject(any(String.class), eq(String.class), any(Object[].class));
    }

    /** Public static nested class so Class.forName can resolve it. */
    public static class FakeResolver implements DataScopeResolver {
        private final DataScopeResolver delegate;
        public FakeResolver() { this.delegate = (u, r) -> List.of(); }
        public FakeResolver(DataScopeResolver delegate) { this.delegate = delegate; }
        @Override public List<Long> resolve(Long userId, String resourceType) {
            return delegate.resolve(userId, resourceType);
        }
    }
}
