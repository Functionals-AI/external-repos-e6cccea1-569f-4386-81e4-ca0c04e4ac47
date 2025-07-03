package ai.functionals.api.neura.controller;

import ai.functionals.api.neura.model.commons.AppUser;
import ai.functionals.api.neura.model.commons.UnsubscribeReq;
import ai.functionals.api.neura.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class UnsubscribeController implements BaseController {
    private final UserService userService;

    @GetMapping()
    public AppUser getUnsubscribePage() {
        return getCurrentUser();
    }

    @PostMapping("/unsubscribe")
    public AppUser unsubscribe(@RequestBody UnsubscribeReq request) {
        return userService.unsubscribeUser(request, getCurrentUser());
    }

}
