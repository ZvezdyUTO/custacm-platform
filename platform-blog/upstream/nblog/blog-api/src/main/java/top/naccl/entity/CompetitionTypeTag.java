package top.naccl.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import top.naccl.enums.CompetitionType;

/**
 * 比赛与固定类型标签的关联投影。
 *
 * @author huangbingrui.awa
 */
@Getter
@Setter
@NoArgsConstructor
public class CompetitionTypeTag {
	private Long competitionId;
	private CompetitionType type;
}
