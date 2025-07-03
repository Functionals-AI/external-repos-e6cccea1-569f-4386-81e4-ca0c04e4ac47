package ai.functionals.api.neura.controller;

import ai.functionals.api.neura.jpa.entity.UserWaitlist;
import ai.functionals.api.neura.service.UserWaitlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/pub/v1/waitlist")
public class UserWaitlistController implements BaseController {
    private final UserWaitlistService userWaitlistService;

    @PostMapping
    public UserWaitlist createWaitlist(@RequestBody UserWaitlist userWaitlist) {
        log.info("Creating waitlist entry for user: {}", userWaitlist);
        return userWaitlistService.createUserWaitlist(userWaitlist);
    }
}
