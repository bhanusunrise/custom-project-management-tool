
package com.pmtool.backend.controller;

import com.pmtool.backend.dto.space.CreateSpaceRequest;
import com.pmtool.backend.dto.space.SpaceResponse;
import com.pmtool.backend.dto.space.UpdateSpaceRequest;
import com.pmtool.backend.service.SpaceService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/spaces")
public class SpaceController {

    private final SpaceService service;

    public SpaceController(SpaceService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SpaceResponse create(
        @Valid @RequestBody CreateSpaceRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return service.create(request, jwt.getSubject());
    }

    @GetMapping
    public List<SpaceResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return service.list(jwt.getSubject());
    }

    @GetMapping("/{id}")
    public SpaceResponse get(
        @PathVariable Long id,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return service.get(id, jwt.getSubject());
    }

    @PutMapping("/{id}")
    public SpaceResponse update(
        @PathVariable Long id,
        @Valid @RequestBody UpdateSpaceRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return service.update(id, request, jwt.getSubject());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
        @PathVariable Long id,
        @AuthenticationPrincipal Jwt jwt
    ) {
        service.delete(id, jwt.getSubject());
    }
}
