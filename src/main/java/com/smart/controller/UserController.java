package com.smart.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.repository.query.Param;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import com.smart.dao.ContactRepository;
import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.User;
import com.smart.helper.Message;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/user")
public class UserController {

	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ContactRepository contactRepository;

	// method to adding data to response
	@ModelAttribute
	public void addCommonData(Model model, Principal principal) {
		String userName = principal.getName();
		User user = userRepository.getUserByUserName(userName);
		model.addAttribute("user", user);
	}

	// dashboard home
	@RequestMapping("/index")
	public String dashboard(Model model, Principal principal) {
		removeVerificationMessageFromSession();
		model.addAttribute("title", "User Dashboard");

		return "norml/user_dashboard";
	}

	@RequestMapping("add-contact")
	public String openAddContactForm(Model model) {
		removeVerificationMessageFromSession();
		model.addAttribute("title", "Add Contact");
		model.addAttribute("contact", new Contact());
		return "norml/add_contact_form";
	}

	// remove attribute
	public void removeVerificationMessageFromSession() {
		try {
			HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
					.getRequest();
			HttpSession session = request.getSession();
			session.removeAttribute("message");
		} catch (RuntimeException e) {
			System.out.print("ERROR" + e.getMessage());
			e.setStackTrace(null);
		}
	}

	@PostMapping("/process-contact")
	public String processContact(@ModelAttribute Contact contact, @RequestParam("profileImage") MultipartFile file,
			Principal principal, HttpSession session) {
		removeVerificationMessageFromSession();
		try {
			String name = principal.getName();
			User user = userRepository.getUserByUserName(name);
			// processing and uploading file

			if (file.isEmpty()) {
				System.out.println("file is empty");
				contact.setImage("contact.png");
			} else {
				contact.setImage(file.getOriginalFilename());
				File saveFile = new ClassPathResource("static/img").getFile();
				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());

				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

			}
			contact.setUser(user);
			user.getContacts().add(contact);

			this.userRepository.save(user);
			session.setAttribute("message", new Message("Your contact was added!!", "success"));

		} catch (Exception e) {
			System.out.print("ERROR" + e.getMessage());
			session.setAttribute("message", new Message("Something went wrong!! Try again!", "danger"));
		}
		return "norml/add_contact_form";
	}

	// show contacts
	@GetMapping("/show-contacts")
	public String showContacts(Model model, Principal principal, HttpSession session) {

		model.addAttribute("title", "Your Contacts");
		String username = principal.getName();
		User user = this.userRepository.getUserByUserName(username);
		List<Contact> contacts = this.contactRepository.findContactByUser(user.getId());

		model.addAttribute("contacts", contacts);
		return "norml/show_contacts";
	}

	// showing particular contact detail
	@RequestMapping("/{cId}/contact")
	public String showContactDetail(@PathVariable("cId") Integer cId, Model model, Principal principal) {

		Optional<Contact> contactOptional = this.contactRepository.findById(cId);
		Contact contact = contactOptional.get();

		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);

		if (user.getId() == contact.getUser().getId()) {
			model.addAttribute("contact", contact);
			model.addAttribute("title", contact.getName());
		}

		return "norml/contact_detail";
	}

	// delete user
	@GetMapping("/delete/{cId}")
	public String deleteContact(@PathVariable("cId") Integer cId, Model model, HttpSession session) {

		System.out.print("ID " + cId);
		Contact contact = this.contactRepository.findById(cId).get();

		// check. . Assignment. .
		this.contactRepository.delete(contact);
		contact.setUser(null);
		session.setAttribute("message", new Message("Contact delete succesfully!!", "success"));

		return "redirect:/user/show-contacts";
	}

	// open update form
	@RequestMapping("/update-contact/{cId}")
	public String updateForm(Model model, @PathVariable("cId") Integer cId) {
		model.addAttribute("title", "Update Contact");

		Contact contact = this.contactRepository.findById(cId).get();
		model.addAttribute("contact", contact);

		return "norml/update_form";
	}

	// update contact handler
	@RequestMapping(value = "/process-update", method = RequestMethod.POST)
	public String updateHandler(@ModelAttribute Contact contact, Model model, HttpSession session,
			@RequestParam("profileImage") MultipartFile file, Principal principal) {

		// old contact detail
		Contact oldContactDetail = this.contactRepository.findById(contact.getcId()).get();
		try {
			// image..
			if (!file.isEmpty()) {
				// delete old photo
				File deleteFile = new ClassPathResource("static/img").getFile();
				File file1 = new File(deleteFile, oldContactDetail.getImage());
				file1.delete();

				// add new photo
				File saveFile = new ClassPathResource("static/img").getFile();
				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());

				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				contact.setImage(file.getOriginalFilename());
			} else {
				contact.setImage(oldContactDetail.getImage());
			}
			User user = this.userRepository.getUserByUserName(principal.getName());
			contact.setUser(user);
			this.contactRepository.save(contact);
			session.setAttribute("message", new Message("Your contact is updated...", "success"));

		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("CONTACT NAME " + contact.getName());
		System.out.println("CONTACT ID " + contact.getcId());

		return "redirect:/user/" + contact.getcId() + "/contact";
	}

	// your profile handler
	@GetMapping("/profile")
	public String yourProfile(Model model) {
		removeVerificationMessageFromSession();
		model.addAttribute("title", "Profile Page");
		return "norml/profile";
	}

	// open settings handler
	@GetMapping("/settings")
	public String openSettings() {
		return "norml/settings";
	}

	// change password.. handler
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("oldPassword") String oldPassword,
			@RequestParam("newPassword") String newPassword,
			@RequestParam("confirmNewPassword") String confirmNewPassword, Principal principal, HttpSession session) {

		String userName = principal.getName();
		User currentUser = this.userRepository.getUserByUserName(userName);

		if (this.bCryptPasswordEncoder.matches(oldPassword, currentUser.getPassword())) {
			if (newPassword.equals(confirmNewPassword)) {
				// pass matched
				currentUser.setPassword(this.bCryptPasswordEncoder.encode(newPassword));
				this.userRepository.save(currentUser);
				session.setAttribute("message", new Message("Your password is updated...", "success"));
			} else {
				session.setAttribute("message", new Message("Please enter correct confirm password!!", "danger"));
			}
		} else {
			// pass unmatched
			session.setAttribute("message", new Message("Please enter correct old password!!", "danger"));
		}
		/* System.out.println("name "+currentUser.getPassword()); */
		return "redirect:/user/settings";
	}
}
