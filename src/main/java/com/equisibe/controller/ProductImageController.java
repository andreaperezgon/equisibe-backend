package com.equisibe.controller;

import com.equisibe.model.Role;
import com.equisibe.repository.UserRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Principal;
import java.util.Locale;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ProductImageController {
    private final UserRepository users;
    private final Path directory;

    public ProductImageController(
            UserRepository users,
            @Value("${app.product-images-directory:uploads/products}") String directory) {
        this.users = users;
        this.directory = Path.of(directory).toAbsolutePath().normalize();
    }

    @PostMapping(
            value = "/api/admin/product-images",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImageResponse upload(
            @RequestParam("file") MultipartFile file,
            Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        users.findByEmail(principal.getName())
                .filter(user -> user.isEnabled() && user.getRole() == Role.ADMIN)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.FORBIDDEN));

        if (file.isEmpty() || file.getSize() > 10L * 1024 * 1024) {
            throw invalidImage();
        }

        try (var input = file.getInputStream();
             var imageInput = ImageIO.createImageInputStream(input)) {
            if (imageInput == null) throw invalidImage();
            var readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) throw invalidImage();
            var reader = readers.next();
            try {
                reader.setInput(imageInput);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!format.equals("jpeg") && !format.equals("jpg")
                        && !format.equals("png")) {
                    throw invalidImage();
                }
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0
                        || (long) width * height > 16000000L) {
                    throw invalidImage();
                }

                var image = reader.read(0);
                if (image == null) throw invalidImage();
                String extension = format.equals("png") ? "png" : "jpg";
                String filename = UUID.randomUUID() + "." + extension;
                Files.createDirectories(directory);
                Path destination = directory.resolve(filename);
                try {
                    if (!ImageIO.write(image, extension, destination.toFile())) {
                        throw new IOException("No se pudo guardar la imagen.");
                    }
                } catch (IOException failure) {
                    Files.deleteIfExists(destination);
                    throw failure;
                }
                return new ImageResponse("/api/product-images/" + filename);
            } finally {
                reader.dispose();
            }
        } catch (IOException failure) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No se pudo leer o guardar la imagen. Prueba con otro JPG o PNG."
            );
        }
    }

    @GetMapping("/api/product-images/{filename}")
    public ResponseEntity<Resource> getImage(@PathVariable String filename) {
        if (!filename.matches("[0-9a-f-]{36}\\.(jpg|png)")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        Path file = directory.resolve(filename).normalize();
        if (!file.startsWith(directory) || !Files.isRegularFile(file)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok()
                .header("X-Content-Type-Options", "nosniff")
                .contentType(filename.endsWith(".png")
                        ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG)
                .body(new FileSystemResource(file));
    }

    private ResponseStatusException invalidImage() {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Elige una imagen JPG o PNG de hasta 10 MB y 16 megapíxeles."
        );
    }

    public record ImageResponse(String imageUrl) {
    }
}
