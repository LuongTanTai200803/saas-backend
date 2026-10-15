package com.saasai.repository;
import com.saasai.entity.FileMetadata;
import com.saasai.entity.FileOcrPage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
public interface FileOcrPageRepository
        extends JpaRepository<FileOcrPage, Long> {

    Optional<FileOcrPage> findByFileAndPageIndex(
            FileMetadata file,
            Integer pageIndex
    );

    List<FileOcrPage> findByFileOrderByPageIndexAsc(
            FileMetadata file
    );

    boolean existsByFileAndPageIndex(
            FileMetadata file,
            Integer pageIndex
    );
}