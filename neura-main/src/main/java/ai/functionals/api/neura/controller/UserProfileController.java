package ai.functionals.api.neura.controller;

import ai.functionals.api.neura.jpa.entity.UserEntity;
import ai.functionals.api.neura.model.enums.UserRoles;
import ai.functionals.api.neura.service.UserService;
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/user-profile")
public class UserProfileController implements BaseController {
    private final UserService userService;

    @GetMapping
    public UserEntity getCurrentUserProfile() {
        return userService.findById(getCurrentUser().getId());
    }

    @GetMapping({"/{slug}"})
    public UserEntity getUserProfile(@PathVariable(required = false) String slug) {
        if (StringUtils.isBlank(slug) || StringUtils.equalsIgnoreCase(slug, "me")) {
            return getCurrentUserProfile();
        }
        return userService.findBySlug(slug);
    }

    @GetMapping("/roles")
    public List<UserRoles> getUserRoles() {
        return Lists.newArrayList(UserRoles.values());
    }

}
