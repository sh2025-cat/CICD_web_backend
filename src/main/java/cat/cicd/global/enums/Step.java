package cat.cicd.global.enums;

import lombok.Getter;

public enum Step {
    TEST("test"), SECURITY("security"), BUILD("build"), INFRA("infra"), DEPLOY("deploy");

    @Getter
    private final String value;

    Step(String value) {
        this.value = value;
    }
}
