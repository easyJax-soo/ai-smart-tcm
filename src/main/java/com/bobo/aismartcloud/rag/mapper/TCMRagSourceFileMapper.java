package com.bobo.aismartcloud.rag.mapper;

import com.bobo.aismartcloud.rag.entity.TCMRagSourceFile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * RAG 源文件 Mapper（JDBCTemplate 实现）
 */
@Repository
public class TCMRagSourceFileMapper {

    private final JdbcTemplate jdbcTemplate;

    public TCMRagSourceFileMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<TCMRagSourceFile> ROW_MAPPER = (rs, rowNum) -> {
        List<String> tags = null;
        Array tagsArray = rs.getArray("tags");
        if (tagsArray != null) {
            String[] arr = (String[]) tagsArray.getArray();
            tags = Arrays.asList(arr);
        }
        return TCMRagSourceFile.builder()
                .id(rs.getLong("id"))
                .fileName(rs.getString("file_name"))
                .fileHash(rs.getString("file_hash"))
                .fileSize(rs.getObject("file_size") != null ? rs.getLong("file_size") : null)
                .documentCount(rs.getObject("document_count") != null ? rs.getInt("document_count") : null)
                .category(rs.getString("category"))
                .tags(tags)
                .createdAt(getLocalDateTime(rs, "created_at"))
                .updatedAt(getLocalDateTime(rs, "updated_at"))
                .deleted(rs.getObject("deleted") != null ? rs.getBoolean("deleted") : false)
                .build();
    };

    private static LocalDateTime getLocalDateTime(ResultSet rs, String column) throws SQLException {
        var ts = rs.getTimestamp(column);
        return ts != null ? ts.toLocalDateTime() : null;
    }

    public void insert(TCMRagSourceFile file) {
        jdbcTemplate.update("""
                INSERT INTO ai.ai_source_files
                    (file_name, file_hash, file_size, document_count, category, tags, created_at, updated_at)
                SELECT ?, ?, ?, ?, ?, ?, ?, ?
                WHERE NOT EXISTS (
                    SELECT 1 FROM ai.ai_source_files WHERE file_hash = ? AND deleted = FALSE
                )
                """,
                file.getFileName(),
                file.getFileHash(),
                file.getFileSize(),
                file.getDocumentCount(),
                file.getCategory(),
                file.getTags() != null ? file.getTags().toArray(new String[0]) : null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                file.getFileHash());
    }

    public Optional<TCMRagSourceFile> findByHash(String fileHash) {
        List<TCMRagSourceFile> list = jdbcTemplate.query(
                "SELECT * FROM ai.ai_source_files WHERE file_hash = ? AND deleted = FALSE",
                ROW_MAPPER, fileHash);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public List<TCMRagSourceFile> findAll() {
        return jdbcTemplate.query(
                "SELECT * FROM ai.ai_source_files WHERE deleted = FALSE ORDER BY created_at DESC",
                ROW_MAPPER);
    }

    public void updateDocumentCount(Long id, int count) {
        jdbcTemplate.update("""
                UPDATE ai.ai_source_files
                SET document_count = ?, updated_at = ?
                WHERE id = ? AND deleted = FALSE
                """, count, LocalDateTime.now(), id);
    }

    /**
     * 软删除（标记 deleted = TRUE）
     */
    public void deleteByHash(String fileHash) {
        jdbcTemplate.update("""
                UPDATE ai.ai_source_files
                SET deleted = TRUE, updated_at = ?
                WHERE file_hash = ?
                """, LocalDateTime.now(), fileHash);
    }
}