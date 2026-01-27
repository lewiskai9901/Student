package com.school.management.application.corrective.command;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ResolveActionCommand {
    private Long actionId;
    private String resolutionNote;
    private List<String> attachments;
}
