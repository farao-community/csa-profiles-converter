package com.farao_community.farao.swe_csa.app;

import com.farao_community.farao.swe_csa.api.exception.CsaInvalidDataException;
import com.farao_community.farao.swe_csa.api.resource.CsaRequest;
import com.farao_community.farao.swe_csa.app.s3.S3ArtifactsAdapter;
import com.google.common.base.Suppliers;
import com.powsybl.commons.datasource.MemDataSource;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.ImportConfig;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.api.parameters.JsonCracCreationParameters;
import com.powsybl.openrao.data.crac.io.csaprofiles.craccreator.CsaProfileCracCreationContext;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

@Service
public class CsaProfilesConverterService {
    private static final DateTimeFormatter HOURLY_NAME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd'_'HHmm").withZone(ZoneId.of("UTC"));

    private final S3ArtifactsAdapter s3ArtifactsAdapter;

    public CsaProfilesConverterService(S3ArtifactsAdapter s3ArtifactsAdapter) {
        this.s3ArtifactsAdapter = s3ArtifactsAdapter;
    }

    public CsaRequest makeRequest(MultipartFile csaProfilesArchive, Instant utcInstant) {
        String taskId = null;
        try {
            FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------"));
            Path targetTmpDir = Files.createTempDirectory("rao-integration-temp-dir", attr);
            Path targetTmpPath = Files.createTempFile(targetTmpDir, "profiles", ".zip", attr);
            Files.copy(csaProfilesArchive.getInputStream(), targetTmpPath, StandardCopyOption.REPLACE_EXISTING);

            Network network = getNetworkFromZipPath(targetTmpPath.toAbsolutePath());
            Crac crac = getCsaCracCreationContext(targetTmpPath.toAbsolutePath(), network, OffsetDateTime.ofInstant(utcInstant, ZoneId.of("UTC"))).getCrac();
            taskId = UUID.randomUUID().toString();

            String cracDestinationPath = "/inputs/" + HOURLY_NAME_FORMATTER.format(utcInstant).concat(".json");
            ByteArrayOutputStream cracBaos = new ByteArrayOutputStream();
            crac.write("JSON", cracBaos);
            s3ArtifactsAdapter.uploadFile(cracDestinationPath, new ByteArrayInputStream(cracBaos.toByteArray()));

            String iidmNetworkDestinationPath = "/inputs/" + HOURLY_NAME_FORMATTER.format(utcInstant).concat(".xiidm");
            MemDataSource memDataSource = new MemDataSource();
            network.write("XIIDM", new Properties(), memDataSource);
            s3ArtifactsAdapter.uploadFile(iidmNetworkDestinationPath, memDataSource.newInputStream("", "xiidm"));
            CsaRequest csaRequest = new CsaRequest(taskId, utcInstant.toString(), s3ArtifactsAdapter.generatePreSignedUrl(iidmNetworkDestinationPath), "fake glsk url", s3ArtifactsAdapter.generatePreSignedUrl(cracDestinationPath), s3ArtifactsAdapter.generatePreSignedUrl(cracDestinationPath));
            LoggerFactory.getLogger("CsaProfilesConverterService").info(csaRequest.toString());
            return csaRequest;
        } catch (IOException e) {
            throw new CsaInvalidDataException(taskId, "cannot convert csa profiles zip to csa request", e);
        }
    }

    public static CsaProfileCracCreationContext getCsaCracCreationContext(Path targetTmpPath, Network network, OffsetDateTime offsetDateTime) {
        try (InputStream inputStream = new FileInputStream(targetTmpPath.toFile())) {
            CracCreationParameters importedParameters = JsonCracCreationParameters.read(CsaProfilesConverterService.class.getResourceAsStream("/csa-crac-parameters.json"));
            return (CsaProfileCracCreationContext) Crac.readWithContext(targetTmpPath.getFileName().toString(), inputStream, network, offsetDateTime, importedParameters);
        } catch (IOException e) {
            throw new OpenRaoException(e);
        }
    }

    public static Network getNetworkFromZipPath(Path zipPath) {
        Properties importParams = new Properties();
        importParams.put("iidm.import.cgmes.cgm-with-subnetworks", false);
        return Network.read(zipPath, LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);
    }

}
