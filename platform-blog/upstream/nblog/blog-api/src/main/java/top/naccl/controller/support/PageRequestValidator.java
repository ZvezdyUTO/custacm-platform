package top.naccl.controller.support;

import top.naccl.exception.BadRequestException;

/**
 * 控制器分页参数校验。
 *
 * @author huangbingrui.awa
 */
public final class PageRequestValidator {
	public static final int MAX_PAGE_SIZE = 100;

	private PageRequestValidator() {
	}

	public static void validate(Integer pageNum, Integer pageSize) {
		if (pageNum == null || pageNum < 1) {
			throw new BadRequestException("页码必须大于 0");
		}
		if (pageSize == null || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
			throw new BadRequestException("每页数量必须在 1 到 100 之间");
		}
	}
}
