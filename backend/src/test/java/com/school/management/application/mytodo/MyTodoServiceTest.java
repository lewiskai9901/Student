package com.school.management.application.mytodo;

import com.school.management.infrastructure.extension.MyTodoSourcePlugin;
import com.school.management.infrastructure.extension.MyTodoSourcePlugin.TodoItem;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MyTodoServiceTest {

    private final MyTodoSourcePlugin alpha = new MyTodoSourcePlugin() {
        @Override public String sourceCode() { return "alpha"; }
        @Override public String sourceName() { return "Alpha 源"; }
        @Override public List<TodoItem> fetch(Long userId) {
            return List.of(new TodoItem("alpha:1", "alpha", "Alpha 源",
                "Alpha task", "desc", "HIGH", "/alpha/1",
                LocalDateTime.now().minusHours(1), null));
        }
    };

    private final MyTodoSourcePlugin beta = new MyTodoSourcePlugin() {
        @Override public String sourceCode() { return "beta"; }
        @Override public String sourceName() { return "Beta 源"; }
        @Override public List<TodoItem> fetch(Long userId) {
            return List.of(
                new TodoItem("beta:1", "beta", "Beta 源", "Beta task 1", "", "MEDIUM",
                    "/beta/1", LocalDateTime.now().minusMinutes(30), null),
                new TodoItem("beta:2", "beta", "Beta 源", "Beta task 2", "", "LOW",
                    "/beta/2", LocalDateTime.now().minusDays(1), null));
        }
    };

    @Test
    void listAll_aggregatesFromAllSources() {
        MyTodoService service = new MyTodoService(List.of(alpha, beta));
        List<TodoItem> result = service.listAll(7L, null);
        assertThat(result).hasSize(3);
        // 倒序 by createdAt: beta:1 (30 min ago) → alpha:1 (1 hr ago) → beta:2 (1 day ago)
        assertThat(result.get(0).id()).isEqualTo("beta:1");
        assertThat(result.get(1).id()).isEqualTo("alpha:1");
        assertThat(result.get(2).id()).isEqualTo("beta:2");
    }

    @Test
    void listAll_filtersBySource() {
        MyTodoService service = new MyTodoService(List.of(alpha, beta));
        List<TodoItem> result = service.listAll(7L, "beta");
        assertThat(result).hasSize(2);
        assertThat(result.stream().allMatch(t -> t.sourceCode().equals("beta"))).isTrue();
    }

    @Test
    void countBySource_returnsCountsForAllSources() {
        MyTodoService service = new MyTodoService(List.of(alpha, beta));
        Map<String, Integer> counts = service.countBySource(7L);
        assertThat(counts.get("alpha")).isEqualTo(1);
        assertThat(counts.get("beta")).isEqualTo(2);
    }

    @Test
    void sourceFailure_doesNotBreakOthers() {
        MyTodoSourcePlugin broken = new MyTodoSourcePlugin() {
            @Override public String sourceCode() { return "broken"; }
            @Override public String sourceName() { return "Broken 源"; }
            @Override public List<TodoItem> fetch(Long userId) {
                throw new RuntimeException("intentional failure");
            }
        };
        MyTodoService service = new MyTodoService(List.of(alpha, broken));
        List<TodoItem> result = service.listAll(7L, null);
        assertThat(result).hasSize(1);  // alpha 还是返了
        assertThat(result.get(0).id()).isEqualTo("alpha:1");
    }
}
