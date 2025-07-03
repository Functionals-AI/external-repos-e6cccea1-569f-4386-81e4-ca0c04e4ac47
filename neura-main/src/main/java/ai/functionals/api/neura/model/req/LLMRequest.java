package ai.functionals.api.neura.model.req;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LLMRequest {
    private String prompt;
    private String sectionName;
    private String sectionPath;
}
