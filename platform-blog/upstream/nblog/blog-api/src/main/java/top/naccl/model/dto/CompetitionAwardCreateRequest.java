package top.naccl.model.dto;

import java.util.List;

/**
 * @author huangbingrui.awa
 */
public record CompetitionAwardCreateRequest(
		String awardMode,
		String teamName,
		String awardTier,
		Integer rankPosition,
		Integer rankTotal,
		List<String> recipientUsernames
) {
}
