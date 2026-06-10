package com.bingmerfest.wordle.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.bingmerfest.wordle.engine.EntropySolver;

@Controller
public class HomeController {

	private final EntropySolver solver;

	public HomeController(EntropySolver solver) {
		this.solver = solver;
	}

	@GetMapping("/")
	public String home(Model model) {
		model.addAttribute("answerCount", solver.words().answerCount());
		model.addAttribute("guessCount", solver.words().guessCount());
		model.addAttribute("opener", solver.config().opener());
		return "home";
	}
}
