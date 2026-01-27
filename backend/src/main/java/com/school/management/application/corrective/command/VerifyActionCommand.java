package com.school.management.application.corrective.command;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VerifyActionCommand {
    private Long actionId;
    private Long verifierId;
    private String result; // "PASS" or "FAIL"
    private String comment;
}
