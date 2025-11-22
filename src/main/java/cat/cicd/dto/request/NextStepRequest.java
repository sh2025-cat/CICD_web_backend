package cat.cicd.dto.request;

import cat.cicd.global.enums.Step;

public record NextStepRequest(Step step) {
}
