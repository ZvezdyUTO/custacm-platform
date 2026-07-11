package top.naccl.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import top.naccl.model.vo.HomepageBannerImage;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

/**
 * 首页横幅图片的 JDBC 持久化适配器。
 *
 * @author huangbingrui.awa
 */
@Repository
public class HomepageBannerRepository {
    private final JdbcTemplate jdbcTemplate;

    public HomepageBannerRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<HomepageBannerImage> findAll() {
        return jdbcTemplate.query("""
                        SELECT id, image_url, sort_order
                        FROM homepage_banner_image
                        ORDER BY sort_order, id
                        """,
                (rs, rowNum) -> new HomepageBannerImage(
                        rs.getLong("id"),
                        rs.getString("image_url"),
                        rs.getInt("sort_order")));
    }

    public Optional<HomepageBannerImage> findById(long id) {
        return jdbcTemplate.query("""
                        SELECT id, image_url, sort_order
                        FROM homepage_banner_image
                        WHERE id = ?
                        """,
                (rs, rowNum) -> new HomepageBannerImage(
                        rs.getLong("id"),
                        rs.getString("image_url"),
                        rs.getInt("sort_order")), id).stream().findFirst();
    }

    public HomepageBannerImage insert(String imageUrl) {
        int sortOrder = Optional.ofNullable(jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(sort_order), -1) + 1 FROM homepage_banner_image", Integer.class)).orElse(0);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO homepage_banner_image (image_url, sort_order) VALUES (?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, imageUrl);
            statement.setInt(2, sortOrder);
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("首页图片写入后未返回主键");
        }
        return new HomepageBannerImage(key.longValue(), imageUrl, sortOrder);
    }

    public void replaceOrder(List<Long> ids) {
        // 先偏移旧顺序，避免唯一索引在逐条更新时发生临时冲突。
        jdbcTemplate.update("UPDATE homepage_banner_image SET sort_order = sort_order + 1000000");
        for (int index = 0; index < ids.size(); index++) {
            jdbcTemplate.update("UPDATE homepage_banner_image SET sort_order = ? WHERE id = ?", index, ids.get(index));
        }
    }

    public int delete(long id) {
        return jdbcTemplate.update("DELETE FROM homepage_banner_image WHERE id = ?", id);
    }

    public int count() {
        return Optional.ofNullable(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM homepage_banner_image", Integer.class)).orElse(0);
    }
}
