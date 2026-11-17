package ch.bbw.trips.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
@Profile("s3")
public class S3ProfilePictureStorage implements ProfilePictureStorage {
	private static final String KEY_PREFIX = "employee-photos/";

	private final S3Client s3Client;
	private final String bucket;
	private final ProfilePictureValidator validator;

	public S3ProfilePictureStorage(
			S3Client s3Client,
			@Value("${app.storage.s3.bucket}") String bucket,
			ProfilePictureValidator validator) {
		if (bucket.isBlank()) {
			throw new IllegalArgumentException("APP_S3_BUCKET muss gesetzt sein, wenn das s3-Profil aktiv ist");
		}
		this.s3Client = s3Client;
		this.bucket = bucket;
		this.validator = validator;
	}

	@Override
	public StoredPicture store(MultipartFile file) throws IOException {
		ProfilePictureValidator.ValidatedPicture picture = validator.validate(file);
		String key = KEY_PREFIX + UUID.randomUUID() + "." + picture.extension();
		s3Client.putObject(
				PutObjectRequest.builder()
						.bucket(bucket)
						.key(key)
						.contentType(picture.contentType())
						.serverSideEncryption("AES256")
						.build(),
				RequestBody.fromBytes(picture.content()));
		return new StoredPicture(key, picture.contentType());
	}

	@Override
	public byte[] load(String key) {
		return s3Client.getObject(
				GetObjectRequest.builder().bucket(bucket).key(requireManagedKey(key)).build(),
				ResponseTransformer.toBytes()).asByteArray();
	}

	@Override
	public void delete(String key) {
		s3Client.deleteObject(DeleteObjectRequest.builder()
				.bucket(bucket)
				.key(requireManagedKey(key))
				.build());
	}

	private String requireManagedKey(String key) {
		return ProfilePictureStorage.requireManagedKey(key);
	}
}
