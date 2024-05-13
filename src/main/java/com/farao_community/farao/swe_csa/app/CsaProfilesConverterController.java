
package com.farao_community.farao.swe_csa.app;

import com.farao_community.farao.swe_csa.api.JsonApiConverter;
import com.farao_community.farao.swe_csa.api.resource.CsaRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;

@RestController
@RequestMapping("/csa-profiles-converter")
@CrossOrigin(origins = "*")
public class CsaProfilesConverterController {

    private static final String JSON_API_MIME_TYPE = "application/vnd.api+json";

    private final CsaProfilesConverterService csaProfilesConverterService;
    private final JsonApiConverter jsonApiConverter = new JsonApiConverter();

    public CsaProfilesConverterController(CsaProfilesConverterService csaProfilesConverterService) {
        this.csaProfilesConverterService = csaProfilesConverterService;
    }

    @PostMapping(value = "/convert-csa-profiles-to-request", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE}, produces = JSON_API_MIME_TYPE)
    public ResponseEntity convertCsaProfilesZipToCsaRequest(@RequestParam MultipartFile csaProfilesArchive, @RequestParam String utcInstant) {
        Instant instant = Instant.parse(utcInstant);
        return ResponseEntity.ok().body(jsonApiConverter.toJsonMessage(csaProfilesConverterService.makeRequest(csaProfilesArchive, instant), CsaRequest.class));
    }
}
