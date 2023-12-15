package com.fherdelpino.uploadfiles.controller;

import com.fherdelpino.uploadfiles.service.StorageService;
import com.fherdelpino.uploadfiles.service.exception.StorageFileNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Controller
public class FileUploadController {

    private final StorageService storageService;

    @GetMapping("/")
    public String listUploadedFiles(Model model) {

        Map<String, String> files = storageService.loadAll()
                .map(Path::toString)
                .collect(Collectors.toMap(Function.identity(), this::getUri, (o1, o2) -> o1, LinkedHashMap::new));

        model.addAttribute("files", files);

        return "uploadForm";
    }

    private String getUri(String fileName) {
        return MvcUriComponentsBuilder.fromMethodName(
                        FileUploadController.class,
                        "serveFile", fileName)
                .build()
                .toUriString();
    }

    @GetMapping("/files/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {

        Resource file = storageService.loadAsResource(filename);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + file.getFilename() + "\"").body(file);
    }

    @PostMapping("/")
    public String handleFileUpload(@RequestParam("files") MultipartFile[] files,
                                   RedirectAttributes redirectAttributes) {
        List<String> filenames = new ArrayList<>();
        for (var file : files) {
            storageService.store(file);
            filenames.add(file.getOriginalFilename());
        }
        redirectAttributes.addFlashAttribute("message",
                String.format("You successfully uploaded %s!", filenames));

        return "redirect:/";
    }

    @ExceptionHandler(StorageFileNotFoundException.class)
    public ResponseEntity<?> handleStorageFileNotFound(StorageFileNotFoundException exc) {
        return ResponseEntity.notFound().build();
    }
}
