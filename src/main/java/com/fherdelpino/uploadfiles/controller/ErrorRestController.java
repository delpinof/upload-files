package com.fherdelpino.uploadfiles.controller;

import com.fherdelpino.uploadfiles.service.exception.StorageFileNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class ErrorRestController {

    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    @ExceptionHandler(StorageFileNotFoundException.class)
    public void notFound() {
        //Nothing to do
    }
}
