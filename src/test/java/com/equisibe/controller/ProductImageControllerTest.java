package com.equisibe.controller;

import com.equisibe.model.Role;
import com.equisibe.model.User;
import com.equisibe.repository.UserRepository;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Principal;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.CRC32;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductImageControllerTest {
    @TempDir Path directory;
    UserRepository users;
    User admin;
    ProductImageController controller;
    final Principal principal = () -> "admin@example.com";
    @BeforeEach void setup() {
        users = mock(UserRepository.class);
        admin = new User("Admin", principal.getName(), "encoded");
        ReflectionTestUtils.setField(admin, "role", Role.ADMIN);
        when(users.findByEmail(principal.getName())).thenReturn(Optional.of(admin));
        controller = new ProductImageController(users, directory.toString());
    }
    byte[] bytes(String format) throws IOException {
        var output = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(new BufferedImage(3, 2, BufferedImage.TYPE_INT_RGB), format, output));
        return output.toByteArray();
    }
    void rejects(HttpStatus status, org.junit.jupiter.api.function.Executable action) {
        assertEquals(status, assertThrows(ResponseStatusException.class, action).getStatusCode());
    }
    @ParameterizedTest @ValueSource(strings={"png", "jpg"})
    void storesDecodedImagesWithServerGeneratedNamesAndServesThem(String format) throws Exception {
        var file = new MockMultipartFile("file", "../../chosen." + format,
                "application/octet-stream", bytes(format));
        String url = controller.upload(file, principal).imageUrl();
        assertTrue(url.matches("/api/product-images/[0-9a-f-]{36}\\.(png|jpg)"));
        String name = url.substring(url.lastIndexOf('/') + 1);
        var response = controller.getImage(name);
        assertEquals("nosniff", response.getHeaders().getFirst("X-Content-Type-Options"));
        assertEquals(format.equals("png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG,
                response.getHeaders().getContentType());
        try (var stream = response.getBody().getInputStream()) {
            var decoded = ImageIO.read(stream);
            assertEquals(3, decoded.getWidth());
            assertEquals(2, decoded.getHeight());
        }
        assertTrue(Files.isRegularFile(directory.resolve(name)));
    }
    @Test void rejectsAnonymousUpload() {
        rejects(HttpStatus.UNAUTHORIZED, () -> controller.upload(new MockMultipartFile("file", new byte[0]), null));
    }
    @Test void rejectsMissingNonAdminAndDisabledAccounts() {
        var file = new MockMultipartFile("file", new byte[0]);
        when(users.findByEmail(principal.getName())).thenReturn(Optional.empty());
        rejects(HttpStatus.FORBIDDEN, () -> controller.upload(file, principal));
        when(users.findByEmail(principal.getName())).thenReturn(Optional.of(admin));
        ReflectionTestUtils.setField(admin, "role", Role.CUSTOMER);
        rejects(HttpStatus.FORBIDDEN, () -> controller.upload(file, principal));
        ReflectionTestUtils.setField(admin, "role", Role.ADMIN);
        ReflectionTestUtils.setField(admin, "enabled", false);
        rejects(HttpStatus.FORBIDDEN, () -> controller.upload(file, principal));
    }
    @Test void rejectsEmptyAndOversizedFilesBeforeDecoding() {
        rejects(HttpStatus.BAD_REQUEST, () -> controller.upload(new MockMultipartFile("file", new byte[0]), principal));
        var oversized = new MockMultipartFile("file", new byte[]{1}) {
            @Override public long getSize() { return 10L * 1024 * 1024 + 1; }
        };
        rejects(HttpStatus.BAD_REQUEST, () -> controller.upload(oversized, principal));
    }
    @Test void rejectsNonImageBytesAndUnsupportedFormats() throws Exception {
        rejects(HttpStatus.BAD_REQUEST, () -> controller.upload(
                new MockMultipartFile("file", "fake.png", "image/png", new byte[]{1,2,3}), principal));
        var gif = new MockMultipartFile("file", "image.gif", "image/gif", bytes("gif"));
        rejects(HttpStatus.BAD_REQUEST, () -> controller.upload(gif, principal));
    }
    @Test void rejectsExcessiveDimensionsBeforeAllocatingPixels() throws Exception {
        byte[] png = bytes("png");
        ByteBuffer.wrap(png).putInt(16, 4001).putInt(20, 4000);
        CRC32 crc = new CRC32();
        crc.update(png, 12, 17);
        ByteBuffer.wrap(png).putInt(29, (int) crc.getValue());
        rejects(HttpStatus.BAD_REQUEST, () -> controller.upload(
                new MockMultipartFile("file", "large.png", "image/png", png), principal));
    }
    @Test void translatesReadAndStorageFailuresToBadRequest() throws Exception {
        var broken = new MockMultipartFile("file", new byte[]{1}) {
            @Override public InputStream getInputStream() throws IOException { throw new IOException("Read failure"); }
        };
        rejects(HttpStatus.BAD_REQUEST, () -> controller.upload(broken, principal));
        Path blocked = directory.resolve("not-a-directory");
        Files.writeString(blocked, "test");
        var blockedController = new ProductImageController(users, blocked.toString());
        var png = new MockMultipartFile("file", bytes("png"));
        rejects(HttpStatus.BAD_REQUEST, () -> blockedController.upload(png, principal));
    }
    @ParameterizedTest @ValueSource(strings={"../secret.png", "image.png", "invalid", "../../pom.xml"})
    void rejectsInvalidImagePaths(String name) {
        rejects(HttpStatus.NOT_FOUND, () -> controller.getImage(name));
    }
    @Test void returnsNotFoundForMissingFilesAndDirectories() throws Exception {
        String name = UUID.randomUUID() + ".png";
        rejects(HttpStatus.NOT_FOUND, () -> controller.getImage(name));
        Files.createDirectory(directory.resolve(name));
        rejects(HttpStatus.NOT_FOUND, () -> controller.getImage(name));
    }
}
