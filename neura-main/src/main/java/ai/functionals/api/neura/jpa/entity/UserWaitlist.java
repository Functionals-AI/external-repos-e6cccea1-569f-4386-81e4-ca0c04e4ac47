package ai.functionals.api.neura.jpa.entity;

import ai.functionals.api.neura.jpa.converters.TailoredFeatureConverter;
import ai.functionals.api.neura.model.enums.MarketingEmailType;
import ai.functionals.api.neura.model.enums.UserWaitlistStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.util.List;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_waitlist", schema = "neura" )
public class UserWaitlist extends Base {
    @Convert(converter = UserWaitlistStatus.Converter.class)
    private UserWaitlistStatus status;
    private String description;

    private String fullName;
    private String email;

    private String jobRole;             // job role of the user
    private String otherJobRole;        // if jobRole is "other"
    private String teamSize;            // team size
    @Convert(converter = TailoredFeatureConverter.class)
    private List<String> workflow;
    //private String workflow;
    private String otherWorkflow;

    private String projectType;
    private String otherProjectType;

    @Column(name = "tailored_features", columnDefinition = "text")
    @Convert(converter = TailoredFeatureConverter.class)
    private List<String> tailoredFeatures;
    //private String tailoredFeatures;
    private String otherTailoredFeature;

    @Column(name = "mail_subscriptions", columnDefinition = "text")
    @Convert(converter = MarketingEmailType.Converter.class)
    private List<MarketingEmailType> mailSubscriptions;
}
