package ch.bbw.trips.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.unit.DataSize;

import java.io.IOException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Map;

@Component
public class ProfilePictureValidator {
	private static final Map<String, String> EXTENSIONS = Map.of(
			"image/jpeg", "jpg",
			"image/png", "png",
			"image/webp", "webp");
	private static final byte[] PNG_SIGNATURE = HexFormat.of().parseHex("89504e470d0a1a0a");

	private final long maxFileSize;

	public ProfilePictureValidator(@Value("${app.storage.max-file-size:5MB}") DataSize maxFileSize) {
		this.maxFileSize = maxFileSize.toBytes();
	}

	public ValidatedPicture validate(MultipartFile file) throws IOException {
		if (file.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bilddatei ist leer");
		}
		if (file.getSize() > maxFileSize) {
			throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE, "Bilddatei ist zu gross");
		}

		String contentType = file.getContentType();
		String extension = contentType == null ? null : EXTENSIONS.get(contentType);
		if (extension == null) {
			throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Nur JPEG-, PNG- und WebP-Bilder sind erlaubt");
		}

		byte[] content = file.getBytes();
		if (!matchesSignature(contentType, content)) {
			throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Dateiinhalt stimmt nicht mit dem Bildtyp ueberein");
		}
		return new ValidatedPicture(content, contentType, extension);
	}

	private boolean matchesSignature(String contentType, byte[] content) {
		return switch (contentType) {
			case "image/jpeg" -> content.length >= 3
					&& (content[0] & 0xff) == 0xff
					&& (content[1] & 0xff) == 0xd8
					&& (content[2] & 0xff) == 0xff;
			case "image/png" -> content.length >= PNG_SIGNATURE.length
					&& Arrays.equals(Arrays.copyOf(content, PNG_SIGNATURE.length), PNG_SIGNATURE);
			case "image/webp" -> content.length >= 12
					&& new String(content, 0, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("RIFF")
					&& new String(content, 8, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("WEBP");
			default -> false;
		};
	}

	public record ValidatedPicture(byte[] content, String contentType, String extension) {
	}
}
