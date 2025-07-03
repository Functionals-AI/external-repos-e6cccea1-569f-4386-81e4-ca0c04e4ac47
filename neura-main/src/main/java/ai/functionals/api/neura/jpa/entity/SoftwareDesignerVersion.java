package ai.functionals.api.neura.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "software_designer_version", schema = "neura" )
public class SoftwareDesignerVersion extends Base {
    private String sessionId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "software_designer_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_neura_sdversion_designer" ))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private SoftwareDesigner softwareDesigner;

    private Long ownerId;
    private String sectionPaths;
    private String prompt;
    private Integer previousDocumentVersion;
    private Integer currentDocumentVersion;
    private Integer schemaVersion;
    private String llmResponseDescription;
}
