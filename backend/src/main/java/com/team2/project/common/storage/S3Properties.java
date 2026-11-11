package com.team2.project.common.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 파일 저장소 S3 설정 (COMMON-47). 자격 증명은 여기서 받지 않는다 — AWS SDK 기본 자격 증명 체인
 * (운영은 EC2 IAM 역할 또는 AWS_ACCESS_KEY_ID · AWS_SECRET_ACCESS_KEY 환경변수, 로컬은 AWS_PROFILE)을 그대로 쓴다.
 *
 * 버킷이 없으면 업로드만 꺼지고 서버는 정상 기동한다. 기본값을 비워 두는 건 의도적이다 — 환경변수를
 * 빠뜨린 로컬 실행이 운영 버킷에 잘못 쓰지 않게 하기 위해서다.
 *
 * S3 호환 저장소(Supabase Storage 등)를 쓸 때만 아래 두 값을 넣는다. 비어 있으면 AWS S3 그대로 동작한다.
 * - endpoint: S3 프로토콜 주소. 예) https://{ref}.storage.supabase.co/storage/v1/s3
 * - publicBaseUrl: 업로드한 파일의 공개 주소 앞부분(버킷까지). 예) https://{ref}.supabase.co/storage/v1/object/public/{bucket}
 *   S3 호환 저장소는 SDK가 만드는 주소(엔드포인트 + 경로)로 공개 조회가 되지 않아 따로 받는다
 */
@ConfigurationProperties("cloud.aws.s3")
public record S3Properties(String bucket, @DefaultValue("") String dirPrefix, @DefaultValue("") String endpoint,
		@DefaultValue("") String publicBaseUrl) {

	public boolean enabled() {
		return bucket != null && !bucket.isBlank();
	}

	/** S3 호환 저장소 엔드포인트가 설정됐는지 */
	public boolean hasEndpoint() {
		return endpoint != null && !endpoint.isBlank();
	}

	/** 공개 주소 앞부분이 설정됐는지 */
	public boolean hasPublicBaseUrl() {
		return publicBaseUrl != null && !publicBaseUrl.isBlank();
	}

	/** dirPrefix가 있으면 키 앞에 붙이고, "/"로 끝나도록 정리한다 */
	public String resolveKey(String key) {
		if (dirPrefix == null || dirPrefix.isBlank()) {
			return key;
		}
		String prefix = dirPrefix.endsWith("/") ? dirPrefix : dirPrefix + "/";
		return prefix + key;
	}

	/** 공개 주소 앞부분 + 키. 앞부분 끝의 "/"는 하나로 맞춘다 */
	public String publicUrl(String resolvedKey) {
		String base = publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
		return base + "/" + resolvedKey;
	}
}
