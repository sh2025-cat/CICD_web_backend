package cat.cicd.global.enums;

import lombok.Getter;

public enum Step {
    TEST("test"), SAST("sast"), BUILD("build"), INFRA("infra"), DEPLOY("deploy");

    @Getter
    private final String value;

    Step(String value) {
        this.value = value;
    }

    public static Step from(String value) {
        for (Step step : Step.values()) {
            if (step.getValue().equalsIgnoreCase(value)) {
                return step;
            }
        }
        return null;
    }
}
