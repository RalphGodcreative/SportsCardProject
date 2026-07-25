package RGcards.SportsCardProject.service;

import RGcards.SportsCardProject.dao.StorageRepository;
import RGcards.SportsCardProject.entity.Storage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StorageService {

    private final StorageRepository storageRepository;

    public List<Storage> findAllForUser(Long userId) {
        return storageRepository.findByUserIdOrderByIdAsc(userId);
    }

    public Storage create(String name, Long userId) {
        Storage storage = new Storage();
        storage.setName(name);
        storage.setUserId(userId);
        return storageRepository.save(storage);
    }

    public Optional<Storage> findByIdAndUserId(long id, Long userId) {
        return storageRepository.findByIdAndUserId(id, userId);
    }

    public void delete(long id, Long userId) {
        storageRepository.findByIdAndUserId(id, userId)
                .ifPresent(storage -> storageRepository.deleteById(storage.getId()));
    }

    public Optional<Storage> rename(long id, String name, Long userId) {
        return storageRepository.findByIdAndUserId(id, userId)
                .map(storage -> {
                    storage.setName(name);
                    return storageRepository.save(storage);
                });
    }

    public Map<Long, String> findNameMapForUser(Long userId) {
        return findAllForUser(userId).stream()
                .collect(Collectors.toMap(Storage::getId, Storage::getName, (a, b) -> a, LinkedHashMap::new));
    }
}
