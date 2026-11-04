package com.shop.animerch.service.storage;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.io.IOException;

@Service
public class ImageValidationService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final int MAX_WIDTH = 6000;
    private static final int MAX_HEIGHT = 6000;
    private static final long MAX_PIXELS = 20_000_000L;
    public void validate(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IOException("Image file is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IOException("Image size exceeds 5 MB");
        }

        String detectedType = detectImageType(file);
        String declaredType = file.getContentType();

        if (!detectedType.equals(declaredType)) {
            throw new IOException("Image content type does not match file content");
        }

        validateImageContent(file);
    }
    private void validateImageContent(MultipartFile file) throws IOException {

        try (var inputStream = file.getInputStream();
             ImageInputStream imageInput = ImageIO.createImageInputStream(inputStream)) {

            if (imageInput == null) {
                throw new IOException("Cannot read image");
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);

            if (!readers.hasNext()) {
                throw new IOException("Unsupported or corrupted image");
            }

            ImageReader reader = readers.next();

            try {
                reader.setInput(imageInput);

                int width = reader.getWidth(0);
                int height = reader.getHeight(0);

                if (width <= 0 || height <= 0) {
                    throw new IOException("Invalid image dimensions");
                }

                if (width > MAX_WIDTH || height > MAX_HEIGHT) {
                    throw new IOException("Image dimensions exceed the limit");
                }

                long pixels = (long) width * height;

                if (pixels > MAX_PIXELS) {
                    throw new IOException("Image contains too many pixels");
                }

                BufferedImage image = reader.read(0);

                if (image == null) {
                    throw new IOException("Cannot decode image");
                }

            } finally {
                reader.dispose();
            }
        }
    }
    private String detectImageType(MultipartFile file) throws IOException {
        byte[] header = new byte[12];

        try (var inputStream = file.getInputStream()) {
            int bytesRead = inputStream.readNBytes(header, 0, header.length);

            if (bytesRead < 12) {
                throw new IOException("Invalid image file");
            }
        }

        // JPEG
        if ((header[0] & 0xFF) == 0xFF &&
                (header[1] & 0xFF) == 0xD8 &&
                (header[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }

        // PNG
        byte[] pngSignature = {
                (byte) 0x89, 0x50, 0x4E, 0x47,
                0x0D, 0x0A, 0x1A, 0x0A
        };

        if (java.util.Arrays.equals(
                java.util.Arrays.copyOfRange(header, 0, 8),
                pngSignature)) {
            return "image/png";
        }

        // WebP
        if (header[0] == 'R' &&
                header[1] == 'I' &&
                header[2] == 'F' &&
                header[3] == 'F' &&
                header[8] == 'W' &&
                header[9] == 'E' &&
                header[10] == 'B' &&
                header[11] == 'P') {
            return "image/webp";
        }

        throw new IOException("Unsupported image format");
    }
    public String getImageExtension(MultipartFile file) throws IOException {
        return switch (detectImageType(file)) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> throw new IOException("Unsupported image format");
        };
    }
}