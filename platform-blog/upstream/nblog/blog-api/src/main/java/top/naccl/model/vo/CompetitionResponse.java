package top.naccl.model.vo;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

/**
 * 面向公开查询和管理员操作返回的比赛树。
 *
 * @author huangbingrui.awa
 */
public record CompetitionResponse(
		Long id,
		String fullName,
		Integer year,
		LocalDate competitionDate,
		String category,
		String categoryLabel,
		String participationMode,
		String participationModeLabel,
		List<Type> types,
		Date createTime,
		Date deletedAt,
		List<Participant> participants,
		List<Award> awards
) {
	public record Type(String code, String label) {
	}

	public record Participant(
			Long id,
			String username,
			String displayName,
			List<Article> articles
	) {
	}

	public record Article(Long id, String title) {
	}

	public record Award(
			Long id,
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
			boolean requiresLogin,
			List<Recipient> recipients
	) {
	}

	public record Recipient(Long participantId, String username, String displayName) {
	}
}
