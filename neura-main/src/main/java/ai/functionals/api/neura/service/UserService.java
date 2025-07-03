package ai.functionals.api.neura.service;

import ai.functionals.api.neura.jpa.entity.UserCreditAudit;
import ai.functionals.api.neura.jpa.entity.UserEntity;
import ai.functionals.api.neura.jpa.repo.UserCreditAuditRepo;
import ai.functionals.api.neura.jpa.repo.UserRepo;
import ai.functionals.api.neura.model.commons.AppException;
import ai.functionals.api.neura.model.commons.AppUser;
import ai.functionals.api.neura.model.commons.UnsubscribeReq;
import ai.functionals.api.neura.model.enums.UserRoles;
import ai.functionals.api.neura.model.req.UpdateUserReq;
import com.google.common.collect.ImmutableSet;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepo userRepo;
    private final UserCreditAuditRepo userCreditAuditRepo;
    @Value("${app.root.users}")
    private String rootUsersCsv;
    @Getter
    private Set<String> rootUsers;

    @Value("${app.admin.users}")
    private String adminUsersCsv;
    @Getter
    private Set<String> adminUsers;

    @PostConstruct
    public void init() {
        rootUsers = Arrays.stream(rootUsersCsv.split(","))
                .filter(StringUtils::isNotBlank).map(String::trim).map(String::toLowerCase)
                .collect(Collectors.toSet());
        log.info("Root users initialized: {}", rootUsers);

        adminUsers = Arrays.stream(adminUsersCsv.split(","))
                .filter(StringUtils::isNotBlank).map(String::trim).map(String::toLowerCase)
                .collect(Collectors.toSet());
        log.info("Admin users initialized: {}", adminUsers);
    }

    public UserEntity findBySlug(String slug) {
        return userRepo.findBySlug(slug).stream().findFirst()
                .orElse(null);
    }

    public UserEntity findByEmail(String email) {
        return userRepo.findByEmail(email).stream().findFirst()
                .orElse(null); // NOTE: if we throw error, SecurityFilter.doFilterInternal breaks
    }

    public UserEntity findById(Long userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> new AppException("User not found for id: " + userId));
    }

    public Set<UserRoles> findRolesByEmail(String email, String externalId) {
        if (rootUsers.stream().anyMatch(user -> StringUtils.equalsIgnoreCase(user, email))) {
            return ImmutableSet.of(UserRoles.ROLE_FUN_ROOT);
        }

        if (adminUsers.stream().anyMatch(user -> StringUtils.equalsIgnoreCase(user, email))) {
            return ImmutableSet.of(UserRoles.ROLE_FUN_ADMIN);
        }

        Set<UserRoles> userRoles = userRepo.findByEmail(email)
                .stream()
                .findFirst()
                .map(UserEntity::getRoles)
                .orElseGet(() -> ImmutableSet.of(UserRoles.ROLE_FUN_ANONYMOUS));

        userRoles.addAll(userRepo.findByExternalId(externalId)
                .stream()
                .findFirst()
                .map(UserEntity::getRoles)
                .orElseGet(() -> ImmutableSet.of(UserRoles.ROLE_FUN_ANONYMOUS)));

        return userRoles;
    }

    public void useCredits(Long userId, Long amount) {
        log.info("Using credits for userId: {}, amount: {}", userId, amount);
        if (userId == null) {
            log.warn("UserId is null, skipping credit usage update.");
        } else {
            UserEntity user = userRepo.findById(userId).orElse(null);
            if (user != null) {
                long currentUserCredits = user.getUsedCredits() == null ? 0 : user.getUsedCredits();
                long usedCredits = user.getUsedCredits();
                user.setUsedCredits(currentUserCredits + (amount == null ? 0 : amount));
                UserEntity saved = userRepo.save(user);
                UserCreditAudit userCreditAudit = new UserCreditAudit();
                userCreditAudit.setUser(saved);
                userCreditAudit.setUsedCredits(saved.getUsedCredits() == null ? 0L : saved.getUsedCredits());
                userCreditAuditRepo.save(userCreditAudit);
                log.info("Updated used credits for userId: {}, old credits:{}, new used credits: {}", userId, usedCredits, saved.getUsedCredits());
            } else {
                throw new AppException("Not enough credits");
            }
        }
    }

    public AppUser unsubscribeUser(UnsubscribeReq request, AppUser currentUser) {
        log.info("Unsubscribe request received {}", request);
        UserEntity user = userRepo.findByEmail(currentUser.getEmail()).stream().findFirst()
                .orElseThrow(() -> new AppException("User not found"));
        if (StringUtils.isNotBlank(currentUser.getEmail())) {
            user.setMailSubscriptions(request.getMailSubscriptions());
            userRepo.save(user);
        }
        return currentUser;
    }

    public UserEntity persistIfValidUser(String email, String userId, String name, String tenantId, Set<UserRoles> roles) {
        UserEntity user = null;
        if ((StringUtils.isNotBlank(email) || StringUtils.isNotBlank(userId))
                && roles.stream().anyMatch((it) -> UserRoles.ROLE_FUN_USER.equals(it) || UserRoles.ROLE_FUN_ADMIN.equals(it) || UserRoles.ROLE_FUN_ROOT.equals(it))
                && StringUtils.isNotBlank(tenantId)) {
            Optional<UserEntity> userOpt = StringUtils.isNotBlank(email) ? userRepo.findByEmail(email).stream().findFirst()
                    : userRepo.findByExternalId(userId).stream().findFirst();
            if (userOpt.isEmpty()) {
                userOpt = userRepo.findByExternalId(userId).stream().findFirst();
            }
            if (userOpt.isEmpty()) {
                log.info("New User detected, persisting user {}/{}", email, userId);
                user = new UserEntity();
                user.setEmail(email);
                user.setExternalId(userId);
                user.setFullName(name);
                user.setRoles(roles);
                user = userRepo.save(user);
            } else {
                //  TODO: sync roles from jwt to db
                user = userOpt.get();
            }
        } else {
            log.warn("Invalid email or userId, skipping persist");
        }
        return user;
    }

    public UserEntity updateUser(String slug, UpdateUserReq updateUserReq) {
        boolean extraCreditUpdated = false;
        boolean usedCreditUpdated = false;
        boolean creditSubscriptionTierUpdated = false;
        UserEntity userEntity = userRepo.findBySlug(slug).stream().findFirst()
                .orElseThrow(() -> new AppException("User not found for slug: " + slug));
        if (StringUtils.isNotBlank(updateUserReq.getExternalId())) {
            log.info("Updating externalId for user: {}, new externalId: {} (prev: {})", slug, updateUserReq.getExternalId(), userEntity.getExternalId());
            userEntity.setExternalId(updateUserReq.getExternalId());
        }
        if (StringUtils.isNotBlank(updateUserReq.getFullName())) {
            log.info("Updating fullName for user: {}, new fullName: {} (prev: {})", slug, updateUserReq.getFullName(), userEntity.getFullName());
            userEntity.setFullName(updateUserReq.getFullName());
        }
        if (updateUserReq.getRoles() != null && !updateUserReq.getRoles().isEmpty()) {
            log.info("Updating roles for user: {}, new roles: {} (prev: {})", slug, updateUserReq.getRoles(), userEntity.getRoles());
            userEntity.setRoles(updateUserReq.getRoles());
        }
        if (updateUserReq.getMailSubscriptions() != null && !updateUserReq.getMailSubscriptions().isEmpty()) {
            log.info("Updating mail subscriptions for user: {}, new subscriptions: {} (prev: {})", slug, updateUserReq.getMailSubscriptions(), userEntity.getMailSubscriptions());
            userEntity.setMailSubscriptions(updateUserReq.getMailSubscriptions());
        }
        if (updateUserReq.getExtraCredits() != null) {
            log.info("Updating extra credits for user: {}, new extra credits: {} (prev: {})", slug, updateUserReq.getExtraCredits(), userEntity.getExtraCredits());
            extraCreditUpdated = userEntity.getExtraCredits() != null && !updateUserReq.getExtraCredits().equals(userEntity.getExtraCredits());
            userEntity.setExtraCredits(updateUserReq.getExtraCredits());
        }
        if (updateUserReq.getUsedCredits() != null) {
            log.info("Updating used credits for user: {}, new used credits: {} (prev: {})", slug, updateUserReq.getUsedCredits(), userEntity.getUsedCredits());
            usedCreditUpdated = userEntity.getUsedCredits() != null && !updateUserReq.getUsedCredits().equals(userEntity.getUsedCredits());
            userEntity.setUsedCredits(updateUserReq.getUsedCredits());
        }
        if (updateUserReq.getCreditSubscriptionTier() != null) {
            log.info("Updating Credit Subscription Tier for user: {}, new Credit Subscription Tier: {} (prev: {})", slug, updateUserReq.getCreditSubscriptionTier(), userEntity.getCreditSubscriptionTier());
            creditSubscriptionTierUpdated = userEntity.getCreditSubscriptionTier() != null && !updateUserReq.getCreditSubscriptionTier().equals(userEntity.getCreditSubscriptionTier());
            userEntity.setCreditSubscriptionTier(updateUserReq.getCreditSubscriptionTier());
        }
        UserEntity saved = userRepo.save(userEntity);
        if (extraCreditUpdated || usedCreditUpdated || creditSubscriptionTierUpdated) {
            UserCreditAudit userCreditAudit = new UserCreditAudit();
            userCreditAudit.setUser(saved);
            if (extraCreditUpdated) {
                userCreditAudit.setExtraCredits(saved.getExtraCredits() == null ? 0L : saved.getExtraCredits());
            }
            if (usedCreditUpdated) {
                userCreditAudit.setUsedCredits(saved.getUsedCredits() == null ? 0L : saved.getUsedCredits());
            }
            if (creditSubscriptionTierUpdated) {
                userCreditAudit.setCreditSubscriptionTier(saved.getCreditSubscriptionTier() == null ? 1L : saved.getCreditSubscriptionTier());
            }
            userCreditAuditRepo.save(userCreditAudit);
        }
        return saved;
    }

    public void validateUserCredits(AppUser user) {
        UserEntity userEntity = userRepo.findById(user.getId()).orElseThrow(() -> new AppException("User not found for id: " + user.getId()));
        Long totalCredits = userEntity.getTotalCredits();
        Long extraCredits = userEntity.getExtraCredits();
        Long usedCredits = userEntity.getUsedCredits();
        if (usedCredits == null || usedCredits < 0) {
            usedCredits = 0L;
        }
        if (totalCredits == null) {
            totalCredits = 0L;
        }
        if (extraCredits == null) {
            extraCredits = 0L;
        }
        long availableCredits = totalCredits + extraCredits - usedCredits;
        if (availableCredits <= -100) { // Allow a leniency of 100 credits for edge case
            log.warn("User {} has insufficient credits: total: {}, extra: {}, used: {}, available: {}",
                    user.getEmail(), totalCredits, extraCredits, usedCredits, availableCredits);
            throw new AppException("Insufficient credits for " + user.getEmail() + "[" + usedCredits + "/" + (totalCredits + extraCredits) + "]");
        } else {
            log.info("User {} has sufficient credits: total: {}, extra: {}, used: {}, available: {}",
                    user.getEmail(), totalCredits, extraCredits, usedCredits, availableCredits);
        }
    }

    public Page<UserEntity> searchUsersByEmail(String email, Pageable pageable) {
        if (StringUtils.isBlank(email)) {
            throw new AppException("Email cannot be blank");
        }
        log.info("Searching users by email: {}", email);
        return userRepo.findByEmailContainingIgnoreCase(email, pageable);
    }

    public List<UserEntity> findByRole(UserRoles userRoles) {
        return userRepo.findByRolesContainingIgnoreCase(userRoles.toString());
    }
}
