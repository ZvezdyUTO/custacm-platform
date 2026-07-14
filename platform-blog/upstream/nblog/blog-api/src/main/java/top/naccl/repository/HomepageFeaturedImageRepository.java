package top.naccl.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import top.naccl.model.vo.HomepageFeaturedImage;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

/**
 * 首页精选图片的 JDBC 持久化适配器。
 *
 * @author huangbingrui.awa
 */
@Repository
public class HomepageFeaturedImageRepository {
    private final JdbcTemplate jdbcTemplate;

    public HomepageFeaturedImageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<HomepageFeaturedImage> findAll() {
        return jdbcTemplate.query("""
                        SELECT id, image_url, thumbnail_url, sort_order
                        FROM homepage_featured_image
                        ORDER BY sort_order, id
                        """,
                (rs, rowNum) -> new HomepageFeaturedImage(
                        rs.getLong("id"),
                        rs.getString("image_url"),
                        rs.getString("thumbnail_url"),
                        rs.getInt("sort_order")));
    }

    public Optional<HomepageFeaturedImage> findById(long id) {
        return jdbcTemplate.query("""
                        SELECT id, image_url, thumbnail_url, sort_order
                        FROM homepage_featured_image
                        WHERE id = ?
                        """,
                (rs, rowNum) -> new HomepageFeaturedImage(
                        rs.getLong("id"),
                        rs.getString("image_url"),
                        rs.getString("thumbnail_url"),
                        rs.getInt("sort_order")), id).stream().findFirst();
    }

    public HomepageFeaturedImage insert(String imageUrl, String thumbnailUrl) {
        int sortOrder = Optional.ofNullable(jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(sort_order), -1) + 1 FROM homepage_featured_image", Integer.class)).orElse(0);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO homepage_featured_image (image_url, thumbnail_url, sort_order) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, imageUrl);
            statement.setString(2, thumbnailUrl);
            statement.setInt(3, sortOrder);
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("精选图片写入后未返回主键");
        }
        return new HomepageFeaturedImage(key.longValue(), imageUrl, thumbnailUrl, sortOrder);
    }

    public void updateThumbnailUrl(long id, String thumbnailUrl) {
        jdbcTemplate.update(
                "UPDATE homepage_featured_image SET thumbnail_url = ? WHERE id = ? AND thumbnail_url IS NULL",
                thumbnailUrl,
                id);
    }

    public void replaceOrder(List<Long> ids) {
        jdbcTemplate.update("UPDATE homepage_featured_image SET sort_order = sort_order + 1000000");
        for (int index = 0; index < ids.size(); index++) {
            jdbcTemplate.update(
                    "UPDATE homepage_featured_image SET sort_order = ? WHERE id = ?",
                    index,
                    ids.get(index));
        }
    }

    public int delete(long id) {
        return jdbcTemplate.update("DELETE FROM homepage_featured_image WHERE id = ?", id);
    }

    public int count() {
        return Optional.ofNullable(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM homepage_featured_image", Integer.class)).orElse(0);
    }
}
