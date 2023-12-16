package com.fherdelpino.uploadfiles.controller;

import com.fherdelpino.uploadfiles.controller.model.FileList;
import com.fherdelpino.uploadfiles.controller.model.FileNamePath;
import com.fherdelpino.uploadfiles.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.fherdelpino.uploadfiles.configuration.FileUploadRestApi.FILE_RELATIVE_PATH;

@RestController
@RequestMapping(FILE_RELATIVE_PATH)
@RequiredArgsConstructor
public class FileUploadRestController {

    private final StorageService storageService;

    @PostMapping
    public ResponseEntity<Object> uploadFile(@RequestParam("file") MultipartFile file) {
        storageService.store(file);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @GetMapping
    @ResponseBody
    public FileList getAllFileNames() {
        List<FileNamePath> files = new ArrayList<>();
        List<Path> fileNames = storageService.loadAll().collect(Collectors.toList());
        for (Path fileName : fileNames) {
            UriComponentsBuilder currentRequestUriPath = ServletUriComponentsBuilder.fromCurrentRequestUri()
                    .path("/" + fileName.toString());
            String uriString = currentRequestUriPath.toUriString();
            files.add(FileNamePath.builder()
                    .name(fileName.toString())
                    .path(uriString)
                    .build());
        }
        return FileList.builder()
                .files(files)
                .build();
    }

    @GetMapping("/{filename}")
    public ResponseEntity<Resource> getFile(@PathVariable String filename) {
        Resource resource = storageService.loadAsResource(filename);
        String headerValue = String.format("attachment; filename=%s", resource.getFilename());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, headerValue)
                .body(resource);
    }

    @DeleteMapping("/{filename}")
    public ResponseEntity<Object> deleteFile(@PathVariable String filename) throws IOException {
        HttpStatus resultStatus;
        if (storageService.delete(filename)) {
            resultStatus = HttpStatus.OK;
        } else {
            resultStatus = HttpStatus.NOT_FOUND;
        }
        return ResponseEntity.status(resultStatus).build();
    }
}
