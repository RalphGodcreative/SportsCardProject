package RGcards.SportsCardProject.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Marks a session as midway through connecting Google to an account that is already
 * signed in, so the OAuth2 callback links the two instead of signing someone in.
 *
 * <p>The marker is only ever set by a CSRF-protected POST from the profile page, so a
 * stray link to the Google authorization endpoint cannot trigger a link on its own.
 */
public final class AccountLinkSession {

    private static final String ATTRIBUTE = "GOOGLE_LINK_USER_ID";

    private AccountLinkSession() {
    }

    public static void start(HttpServletRequest request, Long userId) {
        request.getSession().setAttribute(ATTRIBUTE, userId);
    }

    /** Id of the user asking to connect Google, or null on an ordinary sign-in. */
    public static Long pendingUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (Long) session.getAttribute(ATTRIBUTE);
    }

    /** Same as {@link #pendingUserId(HttpServletRequest)} for code with no request to hand. */
    public static Long pendingUserId() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            return null;
        }
        return pendingUserId(servletAttributes.getRequest());
    }

    public static void clear(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(ATTRIBUTE);
        }
    }
}
