package com.redis.riot.file;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Base64;

import org.springframework.cloud.gcp.autoconfigure.storage.GcpStorageAutoConfiguration;
import org.springframework.cloud.gcp.core.GcpScope;
import org.springframework.cloud.gcp.core.UserAgentHeaderProvider;
import org.springframework.cloud.gcp.storage.GoogleStorageResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.ServiceOptions;
import com.google.cloud.storage.StorageOptions;

import picocli.CommandLine.Option;

public class GoogleStorageArgs {

	public static final String GOOGLE_STORAGE_PROTOCOL_PREFIX = "gs://";

	public static final GcpScope DEFAULT_SCOPE = GcpScope.STORAGE_READ_ONLY;

	@Option(names = "--gcs-key-file", description = "GCS private key (e.g. /usr/local/key.json).", paramLabel = "<file>")
	private File keyFile;

	@Option(names = "--gcs-project", description = "GCP project id.", paramLabel = "<id>")
	private String projectId;

	@Option(names = "--gcs-key", arity = "0..1", interactive = true, description = "GCS Base64 encoded key.", paramLabel = "<key>")
	private String encodedKey;

	@Option(names = "--gcs-scope", description = "GCP scope (default: ${DEFAULT-VALUE}).", paramLabel = "<scope>", hidden = true)
	private GcpScope scope = DEFAULT_SCOPE;

	public GcpScope getScope() {
		return scope;
	}

	public void setScope(GcpScope scope) {
		this.scope = scope;
	}

	public File getKeyFile() {
		return keyFile;
	}

	public void setKeyFile(File file) {
		this.keyFile = file;
	}

	public String getProjectId() {
		return projectId;
	}

	public void setProjectId(String id) {
		this.projectId = id;
	}

	public String getEncodedKey() {
		return encodedKey;
	}

	public void setEncodedKey(String key) {
		this.encodedKey = key;
	}

	public Resource resource(String location) throws IOException {
		StorageOptions.Builder builder = StorageOptions.newBuilder().setProjectId(ServiceOptions.getDefaultProjectId())
				.setHeaderProvider(new UserAgentHeaderProvider(GcpStorageAutoConfiguration.class));
		if (keyFile != null) {
			InputStream inputStream = Files.newInputStream(keyFile.toPath());
			builder.setCredentials(GoogleCredentials.fromStream(inputStream).createScoped(scope.getUrl()));
		}
		if (encodedKey != null) {
			ByteArrayInputStream stream = new ByteArrayInputStream(Base64.getDecoder().decode(encodedKey));
			builder.setCredentials(GoogleCredentials.fromStream(stream));
		}
		if (projectId != null) {
			builder.setProjectId(projectId);
		}
		return new GoogleStorageResource(builder.build().getService(), location);
	}

	public static boolean isGoogleStorageResource(String location) {
		Assert.notNull(location, "Location must not be null");
		return location.toLowerCase().startsWith(GOOGLE_STORAGE_PROTOCOL_PREFIX);
	}

}
