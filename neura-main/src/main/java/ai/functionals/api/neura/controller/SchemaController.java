package ai.functionals.api.neura.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static ai.functionals.api.neura.util.AppUtil.getClarificationSchemaFromResources;
import static ai.functionals.api.neura.util.AppUtil.getSchemaFromResources;

@Slf4j
@RestController
@RequestMapping("/api/schema")
@RequiredArgsConstructor
public class SchemaController {

    @GetMapping("/software-designer/{version}")
    public String getSchema(@PathVariable(required = false) String version) {
        return getSchemaFromResources(version, false);
    }

    @GetMapping("/clarification/{version}")
    public String getClarificationSchema(@PathVariable(required = false) String version) {
        return getClarificationSchemaFromResources(version);
    }

}
