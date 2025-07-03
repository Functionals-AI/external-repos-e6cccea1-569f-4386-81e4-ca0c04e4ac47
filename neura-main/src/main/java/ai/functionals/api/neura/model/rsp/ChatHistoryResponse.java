package ai.functionals.api.neura.model.rsp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatHistoryResponse {
    private String sessionId;
    private Page<ChatDiff> chatDiffs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatDiff {
        private String previous;
        private String current;
        private Integer version;
        private String prompt;
        private String sectionPath;
        private String llmResponse; // TODO: LLM to generate a description of the change
        private Long updateTime;
    }
}
