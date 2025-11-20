package cat.cicd.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record WorkflowInfoResponse(
    @JsonProperty("total_count") int totalCount,
    @JsonProperty("workflow_runs") List<PipeLinesRsp> workflowRuns
) {
	public record PipeLinesRsp(
			long id,
			String name,
			String event,
			String status,
			String conclusion,
			@JsonProperty("workflow_id") long workflowId,
			@JsonProperty("check_suite_id") long checkSuiteId)
	{}
}
