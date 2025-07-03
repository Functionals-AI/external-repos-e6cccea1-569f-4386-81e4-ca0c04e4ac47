package ai.functionals.api.neura.service;

import ai.functionals.api.neura.jpa.entity.UserCreditAudit;
import ai.functionals.api.neura.jpa.entity.UserEntity;
import ai.functionals.api.neura.jpa.entity.UserWaitlist;
import ai.functionals.api.neura.jpa.repo.UserCreditAuditRepo;
import ai.functionals.api.neura.jpa.repo.UserRepo;
import ai.functionals.api.neura.jpa.repo.UserWaitlistRepo;
import ai.functionals.api.neura.model.commons.AppException;
import ai.functionals.api.neura.model.enums.MarketingEmailType;
import ai.functionals.api.neura.model.enums.UserRoles;
import ai.functionals.api.neura.model.enums.UserWaitlistStatus;
import ai.functionals.api.neura.model.req.ApproveWaitListReq;
import ai.functionals.api.neura.model.req.UpdateUserReq;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.TreeSet;

import static ai.functionals.api.neura.util.AppUtil.DEFAULT_CREDITS;

@Service
@RequiredArgsConstructor
public class UserWaitlistService {
    private final UserWaitlistRepo userWaitlistRepository;
    private final UserService userService;
    private final UserRepo userRepository;
    private final EmailService emailService;
    private final UserCreditAuditRepo userCreditAuditRepo;


    public UserWaitlist createUserWaitlist(UserWaitlist userWaitlist) {
        userWaitlistRepository.findByEmail(userWaitlist.getEmail()).stream().findFirst()
                .ifPresent(existingUser -> {
                    throw new AppException("User already exists in the waitlist" );
                });
        userWaitlist.setStatus(UserWaitlistStatus.PENDING);
        UserWaitlist saved = userWaitlistRepository.save(userWaitlist);
        TreeSet<String> toEmails = Sets.newTreeSet(userService.getRootUsers());
        toEmails.addAll(userService.getAdminUsers());
        emailService.email(toEmails, "User Approval Request",
                "admin-user-approval.ftl", ImmutableMap.<String, Object>builder()
                        .put("url", "https://agents.functionals.ai/admin/waitlist/approve/" + saved.getSlug())
                        .put("email", StringUtils.isBlank(saved.getEmail()) ? "noEmail" : saved.getEmail()).build());

        emailService.email(ImmutableSet.of(saved.getEmail()), "We got your request",
                "user-waitlist-creation.ftl", ImmutableMap.<String, Object>builder()
                        .put("name", StringUtils.isBlank(saved.getFullName()) ? "noName" : saved.getFullName())
                        .put("email", StringUtils.isBlank(saved.getEmail()) ? "noEmail" : saved.getEmail()).build());
        return saved;
    }

    public UserWaitlist getUserWaitlist(String email) {
        return userWaitlistRepository.findByEmail(email).stream().findFirst()
                .orElseThrow(() -> new AppException("User not found in the waitlist for email: " + email));
    }

    public UserWaitlist getUserWaitlistById(String slug) {
        return userWaitlistRepository.findBySlug(slug).stream().findFirst()
                .orElseThrow(() -> new AppException("User not found in the waitlist for: " + slug));
    }

    public void deleteUserWaitlist(String email) {
        UserWaitlist userWaitlist = getUserWaitlist(email);
        userWaitlistRepository.delete(userWaitlist);
    }

    public UserWaitlist approveUserWaitlist(String id, ApproveWaitListReq req) {
        UserWaitlist userWaitlist = getUserWaitlistById(id);
        userWaitlist.setStatus(req.getStatus() == null ? UserWaitlistStatus.APPROVED : req.getStatus());
        userWaitlist.setDescription(req.getDescription());

        UserEntity user = userRepository.findByEmail(userWaitlist.getEmail()).stream().findFirst().orElseGet(UserEntity::new);
        user.setFullName(userWaitlist.getFullName());
        user.setEmail(userWaitlist.getEmail());
        user.setMailSubscriptions(
                userWaitlist.getMailSubscriptions() == null || userWaitlist.getMailSubscriptions().isEmpty() ?
                        Lists.newArrayList(MarketingEmailType.DEFAULT) : userWaitlist.getMailSubscriptions());
        user.setRoles(req.getRoles() == null || req.getRoles().isEmpty() ? ImmutableSet.of(UserRoles.ROLE_FUN_USER) : req.getRoles());
        user.setUsedCredits(0L);
        user.setTotalCredits(DEFAULT_CREDITS);
        userRepository.save(user);
        UserWaitlist saved = userWaitlistRepository.save(userWaitlist);
        emailService.email(ImmutableSet.of(user.getEmail()), "Congratulations! Your waitlist request is approved",
                "user-waitlist-approved.ftl", ImmutableMap.<String, Object>builder()
                        .put("name", StringUtils.isBlank(saved.getFullName()) ? "noName" : saved.getFullName())
                        .put("email", StringUtils.isBlank(saved.getEmail()) ? "noEmail" : saved.getEmail()).build());
        UserCreditAudit userCreditAudit = new UserCreditAudit();
        userCreditAudit.setUser(user);
        userCreditAudit.setUsedCredits(0L);
        userCreditAudit.setTotalCredits(DEFAULT_CREDITS);
        userCreditAudit.setExtraCredits(0L);
        userCreditAudit.setCreditSubscriptionTier(1L);
        userCreditAuditRepo.save(userCreditAudit);
        return saved;
    }

    public Page<UserWaitlist> getUserWaitlistPendingApproval(Pageable pageable) {
        return userWaitlistRepository.findByStatus(UserWaitlistStatus.PENDING, pageable);
    }
}