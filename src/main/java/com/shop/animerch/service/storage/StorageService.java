package com.shop.animerch.service.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface StorageService {

    String store(MultipartFile file, StorageType type) throws IOException;

    void delete(String filename, StorageType type) throws IOException;

    Resource loadAsResource(String filename, StorageType type) throws IOException;
}