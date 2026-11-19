package jp.ac.sanko.controller;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import jp.ac.sanko.entity.User;
import jp.ac.sanko.repository.UserRepository;

@Controller
public class RegisterController {

	private final PasswordEncoder passwordEncoder;
	private final UserRepository userRepository;

	public RegisterController(
			UserRepository userRepository,
			PasswordEncoder passwordEncoder) {

		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	// 新規登録画面の表示
	@RequestMapping(path = "/register", method = RequestMethod.GET)
	public String showRegister() {
		return "/register";
	}

	// 新規登録の実行
	@RequestMapping(path = "/register", method = RequestMethod.POST)
	public String register(RegisterForm form, Model model) {
		String mail = form.getMailAdress() == null ? "" : form.getMailAdress().trim();
		String password = form.getPassword() == null ? "" : form.getPassword();

		if (mail.isEmpty()) {
			return error("メールアドレスを入力してください。", mail, model);
		}
		if (mail.length() > 100) {
			return error("メールアドレスは100文字以内で入力してください。", mail, model);
		}
		if (!mail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
			return error("正しいメールアドレスの形式で入力してください。", mail, model);
		}
		if (password.isEmpty()) {
			return error("パスワードを入力してください。", mail, model);
		}
		if (password.length() < 8) {
			return error("パスワードは8文字以上で入力してください。", mail, model);
		}

		// BCryptで扱うパスワードの長さを制限する
		if (password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
			return error("パスワードはUTF-8で72バイト以内にしてください。", mail, model);
		}
		if (!password.equals(form.getConfirmPassword())) {
			return error("パスワードが一致しません。", mail, model);
		}
		if (userRepository.findByEmail(mail).isPresent()) {
			return error("このメールアドレスは既に登録されています。", mail, model);
		}

		User user = new User();
		user.setEmail(mail);

		// パスワードをBCryptでハッシュ化して保存
		user.setPassword(passwordEncoder.encode(password));

		userRepository.save(user);

		// 登録完了のメッセージを出すため、パラメータをつけてログイン画面へ
		return "redirect:/login?registered";
	}

	// エラー時の共通処理（入力したメールアドレスは残す）
	private String error(String message, String mail, Model model) {
		model.addAttribute("errorMessage", message);
		model.addAttribute("mailAdress", mail);
		return "/register";
	}
}