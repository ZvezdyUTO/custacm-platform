package top.naccl.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 奖项获奖人与参赛人的关联投影。
 *
 * @author huangbingrui.awa
 */
@Getter
@Setter
@NoArgsConstructor
public class CompetitionAwardRecipient {
	private Long competitionId;
	private Long awardId;
	private Long participantId;
	private String username;
	private String displayNameSnapshot;
	private String displayName;
}
