package com.school.management.application.export.command;

public class CreateExportCommand {
    private String exportType;
    private String exportFormat;
    private String filters;  // JSON string of filter parameters
    private Long createdBy;

    private CreateExportCommand() {}

    public static Builder builder() { return new Builder(); }

    public String getExportType() { return exportType; }
    public String getExportFormat() { return exportFormat; }
    public String getFilters() { return filters; }
    public Long getCreatedBy() { return createdBy; }

    public static class Builder {
        private final CreateExportCommand cmd = new CreateExportCommand();
        public Builder exportType(String v) { cmd.exportType = v; return this; }
        public Builder exportFormat(String v) { cmd.exportFormat = v; return this; }
        public Builder filters(String v) { cmd.filters = v; return this; }
        public Builder createdBy(Long v) { cmd.createdBy = v; return this; }
        public CreateExportCommand build() { return cmd; }
    }
}
