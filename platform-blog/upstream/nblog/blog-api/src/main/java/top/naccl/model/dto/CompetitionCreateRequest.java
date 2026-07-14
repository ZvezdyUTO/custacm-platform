package top.naccl.model.dto;

import java.util.List;

/**
 * @author huangbingrui.awa
 */
public record CompetitionCreateRequest(
		String fullName,
		Integer year,
		List<String> types,
		String participationMode
) {
}
