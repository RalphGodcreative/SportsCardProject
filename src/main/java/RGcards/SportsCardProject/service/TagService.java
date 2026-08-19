package RGcards.SportsCardProject.service;

import RGcards.SportsCardProject.dao.TagRepository;
import RGcards.SportsCardProject.entity.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;

    public List<Tag> findAllForUser(Long userId) {
        return tagRepository.findByUserIdOrderByIdAsc(userId);
    }

    public Tag create(String name, Long userId) {
        Tag tag = new Tag();
        tag.setName(name);
        tag.setUserId(userId);
        return tagRepository.save(tag);
    }

    public Optional<Tag> findByIdAndUserId(long id, Long userId) {
        return tagRepository.findByIdAndUserId(id, userId);
    }

    public void delete(long id, Long userId) {
        tagRepository.findByIdAndUserId(id, userId)
                .ifPresent(tag -> tagRepository.deleteById(tag.getId()));
    }

    public Optional<Tag> rename(long id, String name, Long userId) {
        return tagRepository.findByIdAndUserId(id, userId)
                .map(tag -> {
                    tag.setName(name);
                    return tagRepository.save(tag);
                });
    }

    /**
     * Resolve posted tag ids to entities, dropping any the user doesn't own.
     * Never returns null — an empty set is what clears a card's tags.
     */
    public Set<Tag> resolveOwnedTags(List<Long> tagIds, Long userId) {
        if (tagIds == null || tagIds.isEmpty()) return new LinkedHashSet<>();
        return new LinkedHashSet<>(tagRepository.findByIdInAndUserId(tagIds, userId));
    }
}
