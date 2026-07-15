package top.naccl.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import top.naccl.enums.CompetitionParticipationMode;

import java.time.LocalDate;
import java.util.Date;

/**
 * 比赛聚合根。
 *
 * @author huangbingrui.awa
 */
@Getter
@Setter
@NoArgsConstructor
public class Competition {
	private Long id;
	private String fullName;
	private String activeFullName;
	private Integer competitionYear;
	private LocalDate competitionDate;
	private CompetitionParticipationMode participationMode;
	private Date createTime;
	private Date deletedAt;
}
