package top.naccl.model.vo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

/**
 * 文章备份中的评论投影，不包含 IP、邮箱等追踪字段。
 *
 * @author huangbingrui.awa
 */
@Getter
@Setter
@NoArgsConstructor
public class ArticleBackupComment {
	private Long id;
	private Long blogId;
	private Long userId;
	private String username;
	private String nickname;
	private String content;
	private String avatar;
	private Date createTime;
	private String website;
	private Boolean published;
	private Boolean adminComment;
	private Long parentCommentId;
}
