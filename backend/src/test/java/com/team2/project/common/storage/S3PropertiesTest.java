package com.team2.project.common.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

/** 설정 바인딩: 레코드 생성자가 하나여야 Spring이 cloud.aws.s3.* 값을 넣을 수 있다 (생성자를 더하면 기동이 실패한다) */
class S3PropertiesTest {

	private S3Properties bind(Map<String, String> values) {
		return new Binder(new MapConfigurationPropertySource(values)).bind("cloud.aws.s3", S3Properties.class).get();
	}

	@Test
	@DisplayName("버킷만 있으면 나머지는 빈 값으로 채워진다 (AWS S3 그대로)")
	void bind_bucketOnly() {
		S3Properties properties = bind(Map.of("cloud.aws.s3.bucket", "my-bucket"));

		assertThat(properties.bucket()).isEqualTo("my-bucket");
		assertThat(properties.dirPrefix()).isEmpty();
		assertThat(properties.hasEndpoint()).isFalse();
		assertThat(properties.hasPublicBaseUrl()).isFalse();
	}

	@Test
	@DisplayName("S3 호환 저장소 값(endpoint · public-base-url)을 받는다")
	void bind_compatibleStorage() {
		S3Properties properties = bind(Map.of(
			"cloud.aws.s3.bucket", "my-bucket",
			"cloud.aws.s3.endpoint", "https://ref.storage.supabase.co/storage/v1/s3",
			"cloud.aws.s3.public-base-url", "https://ref.supabase.co/storage/v1/object/public/my-bucket"));

		assertThat(properties.hasEndpoint()).isTrue();
		assertThat(properties.publicUrl("case-photos/a.png"))
			.isEqualTo("https://ref.supabase.co/storage/v1/object/public/my-bucket/case-photos/a.png");
	}
}
