package ai.functionals.api.neura.model.rsp;

import ai.functionals.api.neura.model.enums.DocSessionStatus;
import functionals.designer.v1.SoftwareDesigner.ResponseType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LLMResponse {
    private String sessionId;
    private String response;
    private ResponseType responseType;
    private String productDesign;
    private String sectionPath; // TODO: List of string? to accommodate situations where llm changes multiple sections
    private String preDesignerRequest;
    private Integer documentVersion;
    private Integer schemaVersion;
    private Long creditsSpent;
    private DocSessionStatus docSessionStatus;
    private String llmResponseDescription; // TODO: LLM to generate a description of the change
}
