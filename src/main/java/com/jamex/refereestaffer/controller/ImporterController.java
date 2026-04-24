package com.jamex.refereestaffer.controller;

import com.jamex.refereestaffer.model.request.ImportResponse;
import com.jamex.refereestaffer.service.DownloadService;
import com.jamex.refereestaffer.service.ImporterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/importer")
public class ImporterController {

    private static final Logger log = LoggerFactory.getLogger(ImporterController.class);

    static final String EXAMPLE_FILENAME = "example import.csv";

    private final ImporterService importerService;
    private final DownloadService downloadService;

    public ImporterController(ImporterService importerService, DownloadService downloadService) {
        this.importerService = importerService;
        this.downloadService = downloadService;
    }

    @GetMapping(value = "/example", produces = "text/csv")
    public Resource download() {
        log.info("Downloading file " + EXAMPLE_FILENAME);
        return downloadService.downloadFile(EXAMPLE_FILENAME);
    }

    @PostMapping
    public ImportResponse importData(@RequestParam("file") MultipartFile file,
                                     @RequestParam("numberOfQueuesToImport") Short numberOfQueuesToImport) {
        log.info(String.format("Importing data from file \"%s\"", file.getOriginalFilename()));
        return importerService.importData(file, numberOfQueuesToImport);
    }
}
