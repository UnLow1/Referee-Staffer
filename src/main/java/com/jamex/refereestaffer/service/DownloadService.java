package com.jamex.refereestaffer.service;

import com.jamex.refereestaffer.model.exception.DownloadFileException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DownloadService {

    public Resource downloadFile(String filename) {
        var file = getClass().getClassLoader().getResource(filename);
        if (file == null) {
            throw new DownloadFileException(filename);
        }
        return new UrlResource(file);
    }
}
