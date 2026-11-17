package ch.bbw.trips.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@Service
@Profile("!s3")
public class LocalProfilePictureStorage implements ProfilePictureStorage {
	private static final String KEY_PREFIX = "employee-photos/";

	private final Path root;
	private final ProfilePictureValidator validator;

	public LocalProfilePictureStorage(
			@Value("${app.storage.location:./uploads}") String location,
			ProfilePictureValidator validator) throws IOException {
		this.root = Path.of(location).toAbsolutePath().normalize();
		this.validator = validator;
		Files.createDirectories(root.resolve(KEY_PREFIX));
	}

	@Override
	public StoredPicture store(MultipartFile file) throws IOException {
		ProfilePictureValidator.ValidatedPicture picture = validator.validate(file);
		String key = KEY_PREFIX + UUID.randomUUID() + "." + picture.extension();
		Files.write(resolve(key), picture.content(), StandardOpenOption.CREATE_NEW);
		return new StoredPicture(key, picture.contentType());
	}

	@Override
	public byte[] load(String key) {
		try {
			return Files.readAllBytes(resolve(key));
		} catch (IOException exception) {
			throw new IllegalStateException("Profilbild konnte nicht gelesen werden", exception);
		}
	}

	@Override
	public void delete(String key) {
		try {
			Files.deleteIfExists(resolve(key));
		} catch (IOException exception) {
			throw new IllegalStateException("Profilbild konnte nicht geloescht werden", exception);
		}
	}

	private Path resolve(String key) {
		Path path = root.resolve(ProfilePictureStorage.requireManagedKey(key)).normalize();
		return path;
	}
}
