package com.saasai.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.saasai.entity.FileMetadata;

import java.util.Optional;

@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, String> {

    // Tìm FileMetadata theo fileId và userId, trả về Optional để xử lý trường hợp không tìm thấy
    Optional<FileMetadata> findByFileIdAndUser_UserId(
            String fileId,
            String userId
    );

    // Tìm FileMetadata theo fileId, trả về Optional để xử lý trường hợp không tìm thấy
    @Query("SELECT f FROM FileMetadata f JOIN f.user u WHERE u.email = :email")
    Page<FileMetadata> findByUserEmail(@Param("email") String email, Pageable pageable);

    // Tính tổng fileSize của tất cả các file thuộc về userId
    @Query("SELECT COALESCE(SUM(f.fileSize), 0) FROM FileMetadata f JOIN f.user u WHERE u.userId = :userId")
    Long sumFileSizeByUserId(@Param("userId") String userId);

}
