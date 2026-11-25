package RGcards.SportsCardProject.service;

import RGcards.SportsCardProject.dao.UserRepository;
import RGcards.SportsCardProject.entity.User;
import RGcards.SportsCardProject.exception.LimitExceededException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Monthly AI-call limit check and counter, shared by every feature that calls Gemini for a user. */
@Component
@RequiredArgsConstructor
public class AiCallGuard {

    private final UsageLimits usageLimits;
    private final UserRepository userRepository;

    public boolean hasRemaining(User principal) {
        return currentCount(principal) < usageLimits.maxAiCalls(principal);
    }

    public void requireRemaining(User principal) {
        int max = usageLimits.maxAiCalls(principal);
        if (currentCount(principal) >= max) {
            throw new LimitExceededException("Monthly AI call limit reached (" + max + ")");
        }
    }

    /** Count one successful call. */
    public void recordCall(User principal) {
        // principal may be an OAuth2UserPrincipal (a User subclass used only as the
        // security principal, not a JPA entity) -- save a real entity, not the principal
        User user = userRepository.findById(principal.getId()).orElseThrow();
        user.setAiCallCount(user.getAiCallCount() + 1);
        userRepository.save(user);
    }

    /** The principal's count goes stale within a session, so read it from the database. */
    private int currentCount(User principal) {
        return userRepository.findById(principal.getId())
                .map(User::getAiCallCount)
                .orElse(principal.getAiCallCount());
    }
}
