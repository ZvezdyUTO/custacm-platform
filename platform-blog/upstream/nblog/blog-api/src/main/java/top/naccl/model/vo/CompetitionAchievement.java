package top.naccl.model.vo;

import java.util.List;

/**
 * 用户个人名片中的比赛获奖记录。
 *
 * @author huangbingrui.awa
 */
public record CompetitionAchievement(
		Long competitionId,
		String competitionFullName,
		Integer year,
		String category,
		String categoryLabel,
		List<CompetitionResponse.Type> types,
		Long awardId,
		String awardTier,
		String awardTierLabel,
		String awardMode,
		String awardModeLabel,
		String teamName,
		String awardScope,
		String awardScopeLabel,
		Integer awardLevel,
		String awardName,
		Integer rankPosition,
		Integer rankTotal,
		String rank,
		boolean profileVisible,
		Long profileOrder
) {
}
