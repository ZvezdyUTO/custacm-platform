package top.naccl.model.vo;

import java.util.Date;

/**
 * 管理员精选文章选择器候选项。
 *
 * @author huangbingrui.awa
 */
public record HomepageFeaturedCandidate(
		Long id,
		String title,
		String description,
		String firstPicture,
		Date createTime,
		String categoryName,
		String authorUsername,
		String authorNickname,
		String authorAvatar,
		Integer sortOrder,
		boolean available,
		Long featuredGroupId
) {
}
