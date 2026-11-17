package ch.bbw.trips.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@Profile("s3")
public class S3StorageConfiguration {
	@Bean(destroyMethod = "close")
	S3Client s3Client(@Value("${app.storage.s3.region}") String region) {
		return S3Client.builder()
				.region(Region.of(region))
				.credentialsProvider(DefaultCredentialsProvider.create())
				.build();
	}
}
