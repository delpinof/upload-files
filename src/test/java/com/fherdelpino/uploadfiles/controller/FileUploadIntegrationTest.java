package com.fherdelpino.uploadfiles.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fherdelpino.uploadfiles.controller.model.FileList;
import com.fherdelpino.uploadfiles.controller.model.FileNamePath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.reactive.function.BodyInserters;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.fherdelpino.uploadfiles.configuration.FileUploadRestApi.FILE_RELATIVE_PATH;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FileUploadIntegrationTest {

    @LocalServerPort
    private int port;

    @Value("${storage.location}")
    private String testStorageLocation;

    @Autowired
    private ObjectMapper objectMapper;

    private static WebTestClient client;
    private static final String HOST_NAME = "http://localhost:";
    private static final String FILE_NAME = "test-upload.txt";
    private static final String TEST_RESOURCE_PATH = "/com/fherdelpino/uploadfiles";
    private static byte[] FILE_DATA;

    @BeforeEach
    public void setup() throws IOException {
        FILE_DATA = loadFile(TEST_RESOURCE_PATH, FILE_NAME);
        client = WebTestClient.bindToServer().baseUrl(HOST_NAME + port).build();
    }

    @Test
    @Order(1)
    public void testUploadFile() throws IOException {

        MultipartBodyBuilder multipartBodyBuilder = createMultiPart(FILE_NAME, FILE_DATA);

        client.post().uri(FILE_RELATIVE_PATH)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(multipartBodyBuilder.build()))
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    @Order(2)
    public void testGetFileResponseOk() {
        client.get().uri(FILE_RELATIVE_PATH + "/" + FILE_NAME)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .consumeWith(entityExchangeResult -> {
                    String actual = new String(entityExchangeResult.getResponseBody());
                    String expected = new String(FILE_DATA);
                    assertThat(actual).isEqualTo(expected);
                });
    }

    @Test
    @Order(3)
    public void testGetListFileResponseOk() throws IOException {
        String fileName = "another-file.txt";
        byte[] fileData = loadFile(TEST_RESOURCE_PATH, fileName);
        MultipartBodyBuilder multipartBodyBuilder = createMultiPart(fileName, fileData);
        client.post().uri(FILE_RELATIVE_PATH)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(multipartBodyBuilder.build()))
                .exchange();

        FileNamePath file1 = FileNamePath.builder()
                .name(FILE_NAME)
                .path(HOST_NAME + port + FILE_RELATIVE_PATH + "/" + FILE_NAME)
                .build();
        FileNamePath file2 = FileNamePath.builder()
                .name(fileName)
                .path(HOST_NAME + port + FILE_RELATIVE_PATH + "/" + fileName)
                .build();
        List<FileNamePath> files = List.of(file1, file2);
        FileList fileList = FileList.builder()
                .files(files)
                .build();
        String expected = objectMapper.writeValueAsString(fileList);
        client.get().uri(FILE_RELATIVE_PATH)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .json(expected);
    }

    @Test
    @Order(100)
    public void testDeleteFileResponseOk() {
        client.delete().uri(FILE_RELATIVE_PATH + "/" + FILE_NAME)
                .exchange()
                .expectStatus()
                .isOk();
        FileSystemUtils.deleteRecursively(new File(testStorageLocation));
    }

    @Test
    public void testDeleteFileResponseNotFound() {
        client.delete().uri(FILE_RELATIVE_PATH + "/notExistingFile.txt")
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    @Test
    public void testGetFileResponseNotFound() {
        client.get().uri(FILE_RELATIVE_PATH + "/notExistingFile.txt")
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    private byte[] loadFile(String dir, String fileName) throws IOException {
        return FileUploadIntegrationTest.class.getResourceAsStream(dir + "/" + fileName).readAllBytes();
    }

    private MultipartBodyBuilder createMultiPart(String fileName, byte[] data) {
        MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
        multipartBodyBuilder.part("file", data).header("Content-Disposition", "form-data; name=file; filename=" + fileName);
        return multipartBodyBuilder;
    }
}
