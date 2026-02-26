package com.matchimban.matchimban_api.global.docs;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AsyncApiUiController {

	@GetMapping("/asyncapi-ui")
	public String asyncApiUi() {
		return "forward:/asyncapi-ui/index.html";
	}
}
