package top.naccl.model.vo;

import top.naccl.entity.Tag;

import java.util.Date;
import java.util.List;

/**
 * 首页精选组内的文章卡片数据。
 *
 * @author huangbingrui.awa
 */
public record HomepageFeaturedArticle(
		Long id,
		String title,
		String description,
		String firstPicture,
		Date createTime,
		String categoryName,
		String authorUsername,
		String authorNickname,
		String authorAvatar,
		List<Tag> tags,
		Integer sortOrder,
		boolean available
) {
}
