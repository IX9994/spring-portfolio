
package jp.ac.sanko.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class CsrfFilter extends OncePerRequestFilter {

	public static final String TOKEN_KEY = "CSRF_TOKEN";

	private final SecureRandom random = new SecureRandom();

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain)
			throws ServletException, IOException {

		String method = request.getMethod();

		// データを変更しないリクエストではトークンを用意する
		if ("GET".equals(method) || "HEAD".equals(method)) {
			HttpSession session = request.getSession();

			if (session.getAttribute(TOKEN_KEY) == null) {
				byte[] bytes = new byte[32];
				random.nextBytes(bytes);

				String token = Base64.getUrlEncoder()
						.withoutPadding()
						.encodeToString(bytes);

				session.setAttribute(TOKEN_KEY, token);
			}

			filterChain.doFilter(request, response);
			return;
		}

		// POSTなど、データを変更するリクエストを検証する
		HttpSession session = request.getSession(false);

		String expected = session == null
				? null
				: (String) session.getAttribute(TOKEN_KEY);

		String actual = request.getParameter("_csrf");

		if (expected == null || actual == null ||
				!MessageDigest.isEqual(
						expected.getBytes(StandardCharsets.UTF_8),
						actual.getBytes(StandardCharsets.UTF_8))) {

			response.sendError(
					HttpServletResponse.SC_FORBIDDEN,
					"CSRFトークンが無効です。画面を再読み込みしてください。");
			return;
		}

		filterChain.doFilter(request, response);
	}
}
