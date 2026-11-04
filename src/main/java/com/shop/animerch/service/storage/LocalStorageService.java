package com.shop.animerch.service.storage;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.UUID;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
@Service
public class LocalStorageService implements StorageService {

    private final Path rootLocation;
    private final ImageValidationService imageValidationService;
    private static final Pattern FILENAME_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-" +
                    "[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\.(jpg|png|webp)$"
    );
    public LocalStorageService(
            StorageProperties storageProperties,
            ImageValidationService imageValidationService
    ) {
        this.rootLocation = Paths.get(storageProperties.location())
                .toAbsolutePath()
                .normalize();

        this.imageValidationService = imageValidationService;
    }

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(rootLocation.resolve("products"));
        Files.createDirectories(rootLocation.resolve("sellers"));
    }

    @Override
    public String store(MultipartFile file, StorageType type) throws IOException {
        imageValidationService.validate(file);

        String extension = imageValidationService.getImageExtension(file);
        String filename = UUID.randomUUID() + extension;

        Path directory = rootLocation.resolve(type.getDirectory()).normalize();
        Path destination = directory.resolve(filename).normalize();

        if (!directory.startsWith(rootLocation) ||
                !destination.startsWith(directory)) {
            throw new IOException("Invalid file path");
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destination);
        }

        return filename;
    }

    @Override
    public void delete(String filename, StorageType type) throws IOException {

        if (filename == null || !FILENAME_PATTERN.matcher(filename).matches()) {
            throw new IOException("Invalid filename");
        }

        if (type == null) {
            throw new IOException("Storage type is required");
        }

        Path directory = rootLocation.resolve(type.getDirectory()).normalize();

        if (Files.isSymbolicLink(directory)) {
            throw new IOException("Symbolic links are not allowed");
        }

        Path file = directory.resolve(filename).normalize();

        if (!file.startsWith(directory) || file.equals(directory)) {
            throw new IOException("Invalid file path");
        }

        if (Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
            if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("Target is not a regular file");
            }

            Files.delete(file);
        }
    }
    @Override
    public Resource loadAsResource(String filename, StorageType type) throws IOException {

        if (filename == null || !FILENAME_PATTERN.matcher(filename).matches()) {
            throw new IOException("Invalid filename");
        }

        if (type == null) {
            throw new IOException("Storage type is required");
        }

        Path directory = rootLocation.resolve(type.getDirectory()).normalize();

        if (Files.isSymbolicLink(directory)) {
            throw new IOException("Symbolic links are not allowed");
        }

        Path file = directory.resolve(filename).normalize();

        if (!file.startsWith(directory) || file.equals(directory)) {
            throw new IOException("Invalid file path");
        }

        if (Files.isSymbolicLink(file) ||
                !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("File not found");
        }

        return new FileSystemResource(file);
    }
}