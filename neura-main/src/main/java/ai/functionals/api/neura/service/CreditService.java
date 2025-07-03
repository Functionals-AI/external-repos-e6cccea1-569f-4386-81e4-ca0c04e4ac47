package ai.functionals.api.neura.service;

import ai.functionals.api.neura.jpa.entity.UserCreditAudit;
import ai.functionals.api.neura.jpa.entity.UserEntity;
import ai.functionals.api.neura.jpa.repo.UserCreditAuditRepo;
import ai.functionals.api.neura.jpa.repo.UserRepo;
import ai.functionals.api.neura.model.enums.UserRoles;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import static ai.functionals.api.neura.util.AppUtil.DEFAULT_CREDITS;

@Service
@RequiredArgsConstructor
public class CreditService {
    private final UserService userService;
    private final UserRepo userRepo;
    private final UserCreditAuditRepo userCreditAuditRepo;

    @Scheduled(cron = "0 0 0 1 * ?")    // Every month at 00:00 on the first day of the month
    public void issueDefaultCreditsForUsersWithRole() {
        userService.findByRole(UserRoles.ROLE_FUN_SUBSCRIBED)
                .forEach(user -> {
                    long tier = 1;
                    if (user.getCreditSubscriptionTier() != null) {
                        tier = user.getCreditSubscriptionTier();
                    }
                    user.setUsedCredits(0L);
                    user.setTotalCredits(DEFAULT_CREDITS * tier);
                    UserEntity saved = userRepo.save(user);
                    UserCreditAudit userCreditAudit = new UserCreditAudit();
                    userCreditAudit.setUser(saved);
                    userCreditAudit.setUsedCredits(0L);
                    userCreditAudit.setTotalCredits(DEFAULT_CREDITS * tier);
                    userCreditAuditRepo.save(userCreditAudit);
                });
    }
}
