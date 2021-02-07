package com.fherdelpino.uploadfiles;

import com.fherdelpino.uploadfiles.service.StorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class FileUploadIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private StorageService storageService;

    @LocalServerPort
    private int port;

    private final String FILE_NAME = "test-upload.txt";

    @Test
    public void shouldUploadFile() throws Exception {
        ClassPathResource resource = new ClassPathResource(FILE_NAME, getClass());

        MultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
        map.add("file", resource);
        ResponseEntity<String> response = this.restTemplate.postForEntity("/", map,
                String.class);

        assertThat(response.getStatusCode()).isEqualByComparingTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation().toString())
                .startsWith("http://localhost:" + this.port + "/");
        then(storageService).should().store(any(MultipartFile.class));
    }

    @Test
    public void shouldDownloadFile() throws Exception {
        ClassPathResource resource = new ClassPathResource(FILE_NAME, getClass());
        given(this.storageService.loadAsResource(FILE_NAME)).willReturn(resource);

        ResponseEntity<String> response = this.restTemplate
                .getForEntity("/files/{filename}", String.class, FILE_NAME);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .isEqualTo(String.format("attachment; filename=\"%s\"", FILE_NAME));
        assertThat(response.getBody()).isEqualTo("Any random text...");
    }
}
