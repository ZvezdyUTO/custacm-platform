package top.naccl.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import top.naccl.enums.CompetitionAwardMode;
import top.naccl.enums.CompetitionAwardScope;

import java.util.Date;

/**
 * 比赛奖项。
 *
 * @author huangbingrui.awa
 */
@Getter
@Setter
@NoArgsConstructor
public class CompetitionAward {
	private Long id;
	private Long competitionId;
	private CompetitionAwardMode awardMode;
	private String teamName;
	private CompetitionAwardScope awardScope;
	private Integer awardLevel;
	private Integer rankPosition;
	private Integer rankTotal;
	private String awardName;
	private Date createTime;
}
