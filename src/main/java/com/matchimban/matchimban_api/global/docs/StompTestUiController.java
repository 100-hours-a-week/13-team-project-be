package com.matchimban.matchimban_api.global.docs;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class StompTestUiController {

	@GetMapping("/stomp-ui")
	public String stompUi() {
		return "forward:/stomp-ui/index.html";
	}
}
