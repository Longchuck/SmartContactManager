package com.smart.controller;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.User;

@Controller
@RequestMapping("/user")
public class UserController {
	
	@Autowired
    private UserRepository userRepository;
	
	//method to adding data to response
	@ModelAttribute
	public void addCommonData(Model model, Principal principal) {
		String userName = principal.getName();
    	System.out.println("user name: "+ userName);
    	
    	User user = userRepository.getUserByUserName(userName);
    	System.out.println("user: "+ user);
    	
    	model.addAttribute("user",user);
	}
	
	//dashboard home
    @RequestMapping("/index")
    public String dashboard(Model model, Principal principal){
    	model.addAttribute("title", "User Dashboard");
		
        return "norml/user_dashboard";
    }

	@RequestMapping("add-contact")
	public String openAddContactForm(Model model) {
		model.addAttribute("title", "Add Contact");
		model.addAttribute("contact", new Contact());
		return "norml/add_contact_form";
	}
}
