package com.saasai.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;


@Service
public class ScanAiTempStorage {

    private final Path rootDirectory;

    public ScanAiTempStorage(
            @Value("${storage.local.base-path:uploads/}")
            String basePath
    ) {
        this.rootDirectory = Paths.get(basePath)
                .resolve("scan-ai-temp");
    }

    public void save(
            String fileId,
            Integer pageIndex,
            MultipartFile image
    ) throws IOException {

        Path fileDirectory =
                rootDirectory.resolve(fileId);

        Files.createDirectories(fileDirectory);

        String extension =
                resolveExtension(image.getContentType());

        Path imagePath =
                fileDirectory.resolve(
                        String.format(
                                "page-%06d.%s",
                                pageIndex,
                                extension
                        )
                );

        Files.write(
                imagePath,
                image.getBytes()
        );
    }

    public List<Path> listImagesByPageIndexes(
                String fileId,
                List<Integer> pageIndexes
        ) throws IOException {

        Path fileDirectory = rootDirectory.resolve(fileId);

        if (!Files.exists(fileDirectory)) {
                throw new IOException("Không tìm thấy thư mục ảnh tạm");
        }

        List<Path> result = new ArrayList<>();

        for (Integer pageIndex : pageIndexes) {
                String prefix = String.format(
                        "page-%06d.",
                        pageIndex
                );

                try (Stream<Path> paths = Files.list(fileDirectory)) {
                Path imagePath = paths
                        .filter(Files::isRegularFile)
                        .filter(path ->
                                path.getFileName()
                                        .toString()
                                        .startsWith(prefix)
                        )
                        .findFirst()
                        .orElseThrow(() ->
                                new IOException(
                                        "Không tìm thấy ảnh pageIndex="
                                                + pageIndex
                                )
                        );

                result.add(imagePath);
                }
        }

        return result;
        }

        public void saveBatchText(
                String fileId,
                List<Integer> pageIndexes,
                String text
        ) throws IOException {

        Path fileDirectory = rootDirectory.resolve(fileId);
        Files.createDirectories(fileDirectory);

        int firstPageIndex = pageIndexes.stream()
                .min(Integer::compareTo)
                .orElseThrow();

        Path textPath = fileDirectory.resolve(
                String.format(
                        "batch-%06d.txt",
                        firstPageIndex
                )
        );

        Files.writeString(
                textPath,
                text,
                StandardCharsets.UTF_8
        );
        }

        public List<String> listBatchTexts(
                String fileId
        ) throws IOException {

        Path fileDirectory = rootDirectory.resolve(fileId);

        if (!Files.exists(fileDirectory)) {
                return List.of();
        }

        try (Stream<Path> paths = Files.list(fileDirectory)) {
                return paths
                        .filter(Files::isRegularFile)
                        .filter(path ->
                                path.getFileName()
                                        .toString()
                                        .startsWith("batch-")
                        )
                        .sorted()
                        .map(path -> {
                        try {
                                return Files.readString(
                                        path,
                                        StandardCharsets.UTF_8
                                );
                        } catch (IOException exception) {
                                throw new UncheckedIOException(exception);
                        }
                        })
                        .toList();
        }
        }

   public List<Path> listImagesOrderByPageIndex(
        String fileId
) throws IOException {

    Path fileDirectory =
            rootDirectory.resolve(fileId);

    if (!Files.exists(fileDirectory)) {
        return List.of();
    }

    try (Stream<Path> paths =
                 Files.list(fileDirectory)) {

        return paths
                .filter(Files::isRegularFile)
                .filter(path -> {
                    String fileName = path.getFileName()
                            .toString()
                            .toLowerCase();

                    return fileName.startsWith("page-")
                            && (
                                fileName.endsWith(".png")
                                || fileName.endsWith(".jpg")
                                || fileName.endsWith(".jpeg")
                            );
                })
                .sorted()
                .toList();
    }
}

    public void delete(
            String fileId
    ) throws IOException {

        Path fileDirectory =
                rootDirectory.resolve(fileId);

        if (!Files.exists(fileDirectory)) {
            return;
        }

        try (Stream<Path> paths =
                     Files.walk(fileDirectory)) {

            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException exception) {
                            throw new UncheckedIOException(
                                    exception
                            );
                        }
                    });
        }
    }

    private String resolveExtension(
            String contentType
    ) {
        if ("image/jpeg".equalsIgnoreCase(contentType)
                || "image/jpg".equalsIgnoreCase(contentType)) {
            return "jpg";
        }

        if ("image/png".equalsIgnoreCase(contentType)) {
            return "png";
        }

        throw new IllegalArgumentException(
                "Chỉ hỗ trợ ảnh PNG hoặc JPEG"
        );
    }
}