package top.naccl.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

/**
 * 参赛人自行绑定的公开文章投影。
 *
 * @author huangbingrui.awa
 */
@Getter
@Setter
@NoArgsConstructor
public class CompetitionArticle {
	private Long competitionId;
	private Long participantId;
	private Long blogId;
	private String title;
	private Date createTime;
}
