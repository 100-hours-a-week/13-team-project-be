package com.matchimban.matchimban_api.global.health;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Health", description = "헬스 체크 API")
public class HealthController {

    @GetMapping("/ping")
    @Operation(summary = "헬스 체크", description = "서버가 살아있는지 확인한다.")
    @ApiResponse(responseCode = "200", description = "pong")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }

    @GetMapping("/csrf")
    @Operation(summary = "CSRF 토큰 발급", description = "CSRF 쿠키를 발급한다.")
    @ApiResponse(responseCode = "200", description = "csrf_ready")
    public ResponseEntity<String> csrf(CsrfToken csrfToken) {
        csrfToken.getToken();
        return ResponseEntity.ok("csrf_ready");
    }
}
