package com.school.management.workflow;

import com.school.management.infrastructure.extension.Contribution;
import com.school.management.infrastructure.extension.plugins.core.CoreManifest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 5 — WorkflowContribution sealed permit + CoreManifest 示例契约验证.
 */
class WorkflowContributionTest {

    @Test
    void coreManifest_contributesWorkflows() {
        CoreManifest manifest = new CoreManifest();
        long count = manifest.contribute()
            .filter(c -> c instanceof Contribution.WorkflowContribution)
            .count();
        assertThat(count).isGreaterThanOrEqualTo(2);
    }

    @Test
    void workflowContribution_hasCorrectUniqueKey() {
        Contribution.WorkflowContribution wf = new Contribution.WorkflowContribution(
            "CORE", "processes/leave-approval.bpmn20.xml", "请假审批");
        assertThat(wf.uniqueKey()).isEqualTo("workflow:CORE:processes/leave-approval.bpmn20.xml");
    }

    @Test
    void workflowContribution_implementsSealed() {
        // Verify it's a permitted subtype of sealed Contribution
        Contribution wf = new Contribution.WorkflowContribution("CORE", "x.xml", "test");
        assertThat(wf).isInstanceOf(Contribution.WorkflowContribution.class);
    }
}
