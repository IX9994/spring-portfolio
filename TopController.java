package jp.ac.sanko.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class TopController {
	
	@RequestMapping(path = "/top")
	public String top() {
		return "/top";
	}

}
