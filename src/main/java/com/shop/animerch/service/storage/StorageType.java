package com.shop.animerch.service.storage;

public enum StorageType {
    PRODUCTS("products"),
    SELLERS("sellers");

    private final String directory;

    StorageType(String directory) {
        this.directory = directory;
    }

    public String getDirectory() {
        return directory;
    }
}