package top.naccl.model.vo;

import java.util.List;

/**
 * One bounded root-comment page and its flattened replies.
 *
 * @author huangbingrui.awa
 */
public record PageCommentPage(
		int totalPages,
		List<PageComment> comments,
		boolean repliesTruncated
) {
	public PageCommentPage {
		if (totalPages < 0) {
			throw new IllegalArgumentException("totalPages must not be negative");
		}
		comments = List.copyOf(comments);
	}
}
