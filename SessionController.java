package jp.ac.sanko.controller;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jp.ac.sanko.entity.User;
import jp.ac.sanko.repository.UserRepository;

@Controller
public class SessionController {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public SessionController(
			UserRepository userRepository,
			PasswordEncoder passwordEncoder) {

		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@RequestMapping(path = "/login", method = RequestMethod.GET)
	public String doLogin() {
		return "/login";
	}

	@RequestMapping(path = "/login", method = RequestMethod.POST)
	public String doLogin(
			LoginForm loginForm,
			HttpServletRequest request,
			Model model) {
		if (loginForm.getMailAdress() == null || loginForm.getMailAdress().trim().isEmpty()) {
			model.addAttribute("errorMessage", "メールアドレスを入力してください。");
			return "/login";
		} else if (loginForm.getPassword() == null || loginForm.getPassword().trim().isEmpty()) {
			model.addAttribute("errorMessage", "パスワードを入力してください。");
			return "/login";
		} else {
			User user = userRepository.findByEmail(loginForm.getMailAdress().trim()).orElse(null);

			if (user == null || !passwordEncoder.matches(
					loginForm.getPassword(), user.getPassword())) {

				model.addAttribute(
						"errorMessage",
						"メールアドレスまたはパスワードが違います。");
				return "/login";
			}

			// ログイン成功時にセッションを取得し、セッションIDを変更する
			HttpSession loginSession = request.getSession();
			request.changeSessionId();

			// ログイン前に使用したCSRFトークンを破棄する
			loginSession.removeAttribute("CSRF_TOKEN");

			// ログインしたユーザーの情報を保存する
			loginSession.setAttribute("ユーザーID", user.getId());
			loginSession.setAttribute("メールアドレス", user.getEmail());

			return "redirect:/index";
		}
	}

	@RequestMapping(path = "/logout", method = RequestMethod.GET)
	public String logout(HttpSession session) {
		if (session.getAttribute("メールアドレス") == null) {
			return "redirect:/login";
		}
		return "/logout";
	}

	@RequestMapping(path = "/doLogout", method = RequestMethod.POST)
	public String dologout(HttpSession session) {
		session.invalidate();
		return "redirect:/top";
	}

}
