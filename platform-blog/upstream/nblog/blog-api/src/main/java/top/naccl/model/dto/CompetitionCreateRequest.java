package top.naccl.model.dto;

/**
 * @author huangbingrui.awa
 */
public record CompetitionCreateRequest(
		String fullName,
		Integer year,
		String category,
		String participationMode
) {
}
