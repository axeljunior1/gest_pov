package com.erp.products.service;

import com.erp.products.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path uploadPath;

    public FileStorageService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadPath);
        } catch (IOException e) {
            throw new BusinessException("Impossible de créer le dossier uploads");
        }
    }

    public String store(MultipartFile file, String subfolder) {
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new BusinessException("Nom de fichier invalide");
        }
        String extension = "";
        int dot = originalName.lastIndexOf('.');
        if (dot > 0) {
            extension = originalName.substring(dot);
        }
        String storedName = UUID.randomUUID() + extension;
        String relativePath = subfolder + "/" + storedName;

        try {
            Path targetDir = uploadPath.resolve(subfolder);
            Files.createDirectories(targetDir);
            Path target = targetDir.resolve(storedName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return relativePath;
        } catch (IOException e) {
            throw new BusinessException("Erreur lors de l'enregistrement du fichier");
        }
    }

    public void delete(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        try {
            Path target = uploadPath.resolve(relativePath).normalize();
            if (!target.startsWith(uploadPath)) {
                return;
            }
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
            // Fichier déjà absent ou verrouillé — la ligne DB est quand même supprimée
        }
    }
}
