package ai.functionals.api.neura.jpa.entity;

import ai.functionals.api.neura.jpa.converters.ContributorsConverter;
import ai.functionals.api.neura.model.enums.DocSessionStatus;
import ai.functionals.api.neura.model.enums.SoftwareDesignerStatus;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.bind.support.SessionStatus;

import java.util.Set;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "software_designer", schema = "neura")
public class SoftwareDesigner extends Base {
    private String sessionId;

    private Long ownerId;
    private String title;
    private String contributors; // TODO: move to new table
    @Convert(converter = SoftwareDesignerStatus.Converter.class)
    private SoftwareDesignerStatus status;

    @Convert(converter = DocSessionStatus.Converter.class)
    private DocSessionStatus docSessionStatus;

    private Integer documentVersion;
    private Integer schemaVersion;
}
