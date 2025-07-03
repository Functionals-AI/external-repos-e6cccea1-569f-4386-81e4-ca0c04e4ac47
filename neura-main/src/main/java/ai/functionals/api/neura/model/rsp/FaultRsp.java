package ai.functionals.api.neura.model.rsp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaultRsp {
    private String fault;
    private List<Trace> position;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Trace {
        private String fileName;
        private String methodName;
        private int lineNumber;
    }
}
