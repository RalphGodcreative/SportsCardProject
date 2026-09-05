package RGcards.SportsCardProject.service;

import RGcards.SportsCardProject.dao.UserRepository;
import RGcards.SportsCardProject.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final AccountDeletionService accountDeletionService;

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public void changeRole(Long targetId, String newRole, User actingAdmin) {
        requireNotSelf(targetId, actingAdmin, "change your own role");
        User target = userRepository.findById(targetId).orElseThrow();
        requireNotLastAdmin(target, newRole);
        target.setRole(newRole);
        userRepository.save(target);
    }

    public void setEnabled(Long targetId, boolean enabled, User actingAdmin) {
        requireNotSelf(targetId, actingAdmin, "disable your own account");
        User target = userRepository.findById(targetId).orElseThrow();
        target.setEnabled(enabled);
        userRepository.save(target);
    }

    public void setAiCallLimitOverride(Long targetId, Integer override) {
        User target = userRepository.findById(targetId).orElseThrow();
        target.setMaxAiCallsOverride(override);
        userRepository.save(target);
    }

    public void deleteUserAndAllData(Long targetId, User actingAdmin) {
        requireNotSelf(targetId, actingAdmin, "delete your own account");
        accountDeletionService.deleteUserAndAllData(targetId);
    }

    private void requireNotSelf(Long targetId, User actingAdmin, String action) {
        if (targetId.equals(actingAdmin.getId())) {
            throw new IllegalStateException("Cannot " + action);
        }
    }

    private void requireNotLastAdmin(User target, String newRole) {
        boolean losingAdminStatus = "ROLE_ADMIN".equals(target.getRole()) && !"ROLE_ADMIN".equals(newRole);
        if (losingAdminStatus && userRepository.countByRole("ROLE_ADMIN") <= 1) {
            throw new IllegalStateException("Cannot remove the last admin");
        }
    }
}
