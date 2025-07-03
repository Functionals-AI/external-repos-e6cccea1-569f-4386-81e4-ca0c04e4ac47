package ai.functionals.api.neura.controller;

import ai.functionals.api.neura.jpa.entity.UserEntity;
import ai.functionals.api.neura.jpa.entity.UserWaitlist;
import ai.functionals.api.neura.model.req.ApproveWaitListReq;
import ai.functionals.api.neura.model.req.UpdateUserReq;
import ai.functionals.api.neura.service.UserService;
import ai.functionals.api.neura.service.UserWaitlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/adm/v1")
public class AdminController implements BaseController {
    private final UserWaitlistService userWaitlistService;
    private final UserService userService;

    // @PreAuthorize("hasAnyRole('ADMIN','ROOT')") - SecurityFilter has this already
    @GetMapping("/waitlist/approve/{slug}")
    public UserWaitlist viewDetailsOfApproveWaitlist(@PathVariable String slug) {
        log.info("Approve waitlist details for user: {}", slug);
        return userWaitlistService.getUserWaitlistById(slug);
    }
    @PostMapping("/waitlist/approve/{slug}")
    public UserWaitlist approveWaitlist(@PathVariable String slug, @RequestBody ApproveWaitListReq req) {
        log.info("Approving waitlist entry for user: {}, req: {}", slug, req);
        return userWaitlistService.approveUserWaitlist(slug, req);
    }

    @GetMapping("/waitlist/pending")
    public Page<UserWaitlist> getPendingWaitlist(Pageable pageable) {
        log.info("Fetching pending approval users");
        return userWaitlistService.getUserWaitlistPendingApproval(pageable);
    }

    @PutMapping("/user/{slug}")
    public UserEntity updateUserCredits(@PathVariable String slug, @RequestBody UpdateUserReq updateUserReq) {
        log.info("Updating credits for user: {}, credits: {}", slug, updateUserReq.getExtraCredits());
        return userService.updateUser(slug, updateUserReq);
    }

    @GetMapping("/user/search/{email}")
    public Page<UserEntity> searchUsersByEmail(@PathVariable String email, Pageable pageable) {
        log.info("Searching users by email: {}", email);
        return userService.searchUsersByEmail(email, pageable);
    }
}
