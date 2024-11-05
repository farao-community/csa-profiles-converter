package com.farao_community.farao.swe_csa.app.s3;

import com.farao_community.farao.swe_csa.api.exception.CsaInternalException;
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class S3ClientsConfigurations {

    @Value("${s3.artifacts.user}")
    private String artifactsAccessKey;
    @Value("${s3.artifacts.secret}")
    private String artifactsAccessSecret;
    @Value("${s3.artifacts.url}")
    private String artifactsS3Url;
    @Value("${s3.artifacts.bucket}")
    private String artifactsBucket;

    @Bean
    public MinioClient getArtifactsClient() {
        try {
            return MinioClient.builder().endpoint(artifactsS3Url).credentials(artifactsAccessKey, artifactsAccessSecret).build();
        } catch (Exception e) {
            throw new CsaInternalException("unknown-id", "Exception in MinIO client", e);
        }
    }

    public String getArtifactsBucket() {
        return artifactsBucket;
    }

}
