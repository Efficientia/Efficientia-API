package com.example.efficientia.documento.storage;

public class StorageFileNotFoundException extends StorageException {

    public StorageFileNotFoundException() {
        super("Conteúdo não encontrado no storage privado.");
    }
}
