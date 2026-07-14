package top.naccl.model.vo;

import java.util.List;

/**
 * 有序首页精选文章组。
 *
 * @author huangbingrui.awa
 */
public record HomepageFeaturedGroup(
		Long id,
		String title,
		Integer sortOrder,
		boolean complete,
		List<HomepageFeaturedArticle> articles
) {
}
