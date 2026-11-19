
package jp.ac.sanko.security;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import jakarta.servlet.http.HttpSession;

@ControllerAdvice
public class CsrfModelAdvice {

    @ModelAttribute("csrfToken")
    public String csrfToken(HttpSession session) {
        return (String) session.getAttribute(
                CsrfFilter.TOKEN_KEY);
    }
}
