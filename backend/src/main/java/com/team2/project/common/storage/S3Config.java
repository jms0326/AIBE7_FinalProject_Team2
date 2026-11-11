package com.team2.project.common.storage;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

/**
 * S3 업로드 (COMMON-47). AWS SDK v2를 직접 쓴다 — Spring Cloud AWS 같은 자동 설정 레이어를 더하지 않는다.
 * 리전만 설정으로 받고, 자격 증명은 SDK 기본 체인(DefaultCredentialsProvider)을 그대로 쓴다.
 *
 * 엔드포인트가 설정되면 S3 호환 저장소(Supabase Storage 등)로 붙는다.
 * - path-style: 버킷을 도메인이 아니라 경로에 넣는다(호환 저장소는 버킷 서브도메인을 지원하지 않는다)
 * - 체크섬: SDK 2.30부터 기본으로 붙는 CRC 체크섬을 필요할 때만 붙인다 — 호환 저장소가 거부하는 경우가 있어서다
 */
@Configuration
@EnableConfigurationProperties(S3Properties.class)
public class S3Config {

	@Bean
	public S3Client s3Client(@Value("${cloud.aws.region:ap-northeast-2}") String region, S3Properties properties) {
		S3ClientBuilder builder = S3Client.builder().region(Region.of(region));
		if (properties.hasEndpoint()) {
			builder.endpointOverride(URI.create(properties.endpoint()))
				.forcePathStyle(true)
				.requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
				.responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED);
		}
		return builder.build();
	}
}
