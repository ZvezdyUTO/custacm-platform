package top.naccl.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

/**
 * 比赛参赛人。username 在账号删除后为空，展示名快照继续保留历史记录。
 *
 * @author huangbingrui.awa
 */
@Getter
@Setter
@NoArgsConstructor
public class CompetitionParticipant {
	private Long id;
	private Long competitionId;
	private String username;
	private String displayNameSnapshot;
	private String displayName;
	private Date createTime;
}
