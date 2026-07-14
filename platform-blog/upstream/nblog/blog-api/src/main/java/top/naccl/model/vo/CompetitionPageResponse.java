package top.naccl.model.vo;

import java.util.List;

/**
 * @author huangbingrui.awa
 */
public record CompetitionPageResponse(
		int pageNum,
		int pageSize,
		long total,
		int totalPages,
		List<CompetitionResponse> list
) {
}
