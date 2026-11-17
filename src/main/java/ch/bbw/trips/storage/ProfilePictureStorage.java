package ch.bbw.trips.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface ProfilePictureStorage {
	StoredPicture store(MultipartFile file) throws IOException;

	byte[] load(String key);

	void delete(String key);

	static String requireManagedKey(String key) {
		if (key == null || !key.matches("employee-photos/[0-9a-f-]{36}\\.(jpg|png|webp)")) {
			throw new IllegalArgumentException("Ungueltiger Profilbild-Schluessel");
		}
		return key;
	}

	record StoredPicture(String key, String contentType) {
	}
}
