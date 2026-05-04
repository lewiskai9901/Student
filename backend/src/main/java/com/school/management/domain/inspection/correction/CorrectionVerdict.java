package com.school.management.domain.inspection.correction;

import java.util.ArrayList;
import java.util.List;

/**
 * 引擎对单条 SubmissionDetail 的整改判定结果.
 *
 * <p>severity=NONE → 不建议建整改单.
 * trace 记录每层决策, 可写入 insp_corrective_cases.explain_trace_json 供审计.
 */
public class CorrectionVerdict {

    public record TraceEntry(String layer, String rule, String input, String output) {}

    private final Long detailId;
    private final String itemCode;
    private final String itemName;
    private final Severity severity;
    private final double severityScore;
    private final boolean mustCorrect;
    private final int suggestedDeadlineDays;
    private final String reason;
    private final List<TraceEntry> trace;

    private CorrectionVerdict(Builder b) {
        this.detailId = b.detailId;
        this.itemCode = b.itemCode;
        this.itemName = b.itemName;
        this.severity = b.severity;
        this.severityScore = b.severityScore;
        this.mustCorrect = b.mustCorrect;
        this.suggestedDeadlineDays = b.suggestedDeadlineDays;
        this.reason = b.reason;
        this.trace = b.trace;
    }

    public boolean shouldSuggest() {
        return severity != null && severity.requiresCorrection();
    }

    public Long getDetailId() { return detailId; }
    public String getItemCode() { return itemCode; }
    public String getItemName() { return itemName; }
    public Severity getSeverity() { return severity; }
    public double getSeverityScore() { return severityScore; }
    public boolean isMustCorrect() { return mustCorrect; }
    public int getSuggestedDeadlineDays() { return suggestedDeadlineDays; }
    public String getReason() { return reason; }
    public List<TraceEntry> getTrace() { return trace; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long detailId;
        private String itemCode;
        private String itemName;
        private Severity severity = Severity.NONE;
        private double severityScore;
        private boolean mustCorrect;
        private int suggestedDeadlineDays;
        private String reason;
        private List<TraceEntry> trace = new ArrayList<>();

        public Builder detailId(Long v) { this.detailId = v; return this; }
        public Builder itemCode(String v) { this.itemCode = v; return this; }
        public Builder itemName(String v) { this.itemName = v; return this; }
        public Builder severity(Severity v) { this.severity = v; return this; }
        public Builder severityScore(double v) { this.severityScore = v; return this; }
        public Builder mustCorrect(boolean v) { this.mustCorrect = v; return this; }
        public Builder deadlineDays(int v) { this.suggestedDeadlineDays = v; return this; }
        public Builder reason(String v) { this.reason = v; return this; }
        public Builder addTrace(String layer, String rule, String input, String output) {
            this.trace.add(new TraceEntry(layer, rule, input, output));
            return this;
        }

        public CorrectionVerdict build() { return new CorrectionVerdict(this); }
    }
}
