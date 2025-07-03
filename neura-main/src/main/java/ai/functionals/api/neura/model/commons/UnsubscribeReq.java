package ai.functionals.api.neura.model.commons;

import ai.functionals.api.neura.model.enums.MarketingEmailType;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UnsubscribeReq {
    private List<MarketingEmailType> mailSubscriptions; // list of mail subscriptions to have for the user
}
