package com.smart.controller;

import java.util.Random;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class fogotController {
	Random random = new Random(1000);
	
	// email id form open handler
	
	@RequestMapping("/forgot")
	public String openEmailForm() {
		return "/fogot_email_form";
	}

	@PostMapping("/send-otp")
	public String sendOTP(@RequestParam("email") String email) {
//		System.out.println("EMAIL " + email);
		
		// generating otp of 4 digit
	
		int otp = random.nextInt(999999);

		System.out.println("otp " + otp);
		
		//send otp to email
		return "/verify_otp";
	}

}
