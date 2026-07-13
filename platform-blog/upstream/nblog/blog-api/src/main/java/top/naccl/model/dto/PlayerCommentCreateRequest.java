package top.naccl.model.dto;

/**
 * 登录用户发表评论时允许提交的字段。
 *
 * @author huangbingrui.awa
 */
public record PlayerCommentCreateRequest(
		String content,
		Long parentCommentId,
		Long blogId
) {
}
