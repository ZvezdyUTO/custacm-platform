package top.naccl.model.dto;

import java.time.LocalDate;

/**
 * @author huangbingrui.awa
 */
public record CompetitionCreateRequest(
		String fullName,
		LocalDate competitionDate,
		String category,
		String participationMode
) {
}
