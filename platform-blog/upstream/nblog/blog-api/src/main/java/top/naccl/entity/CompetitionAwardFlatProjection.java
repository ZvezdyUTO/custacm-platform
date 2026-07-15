package top.naccl.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import top.naccl.enums.CompetitionAwardMode;
import top.naccl.enums.CompetitionAwardScope;
import top.naccl.enums.CompetitionType;

import java.time.LocalDate;

/**
 * 个人名片获奖记录的扁平查询投影；同一奖项的多个类型标签会产生多行。
 *
 * @author huangbingrui.awa
 */
@Getter
@Setter
@NoArgsConstructor
public class CompetitionAwardFlatProjection {
	private Long competitionId;
	private String competitionFullName;
	private Integer competitionYear;
	private LocalDate competitionDate;
	private CompetitionType competitionType;
	private Long awardId;
	private CompetitionAwardMode awardMode;
	private String teamName;
	private CompetitionAwardScope awardScope;
	private Integer awardLevel;
	private Integer rankPosition;
	private Integer rankTotal;
	private String awardName;
	private Boolean requiresLogin;
	private Boolean profileVisible;
	private Long profileOrder;
}
