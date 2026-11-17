package ch.bbw.trips.storage;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProfilePictureValidatorTest {
	private final ProfilePictureValidator validator =
			new ProfilePictureValidator(DataSize.ofMegabytes(1));

	@Test
	void acceptsJpegAndUsesItsContentRatherThanTheOriginalFilename() throws Exception {
		MockMultipartFile file = new MockMultipartFile(
				"file", "../../profile.png", "image/jpeg", new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff});

		ProfilePictureValidator.ValidatedPicture picture = validator.validate(file);

		assertThat(picture.contentType()).isEqualTo("image/jpeg");
		assertThat(picture.extension()).isEqualTo("jpg");
	}

	@Test
	void rejectsAFileWhoseSignatureDoesNotMatchItsDeclaredImageType() {
		MockMultipartFile file = new MockMultipartFile(
				"file", "image.jpg", "image/jpeg", "not an image".getBytes());

		assertThatThrownBy(() -> validator.validate(file))
				.isInstanceOf(ResponseStatusException.class)
				.extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
				.isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
	}

	@Test
	void rejectsFilesAboveTheConfiguredLimit() {
		ProfilePictureValidator smallLimit = new ProfilePictureValidator(DataSize.ofBytes(2));
		MockMultipartFile file = new MockMultipartFile(
				"file", "image.jpg", "image/jpeg", new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff});

		assertThatThrownBy(() -> smallLimit.validate(file))
				.isInstanceOf(ResponseStatusException.class)
				.extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
				.isEqualTo(HttpStatus.CONTENT_TOO_LARGE);
	}
}
