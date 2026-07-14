package top.naccl.model.dto;

import java.util.List;

/**
 * 首页精选组全量排序请求。
 *
 * @author huangbingrui.awa
 */
public record HomepageFeaturedGroupOrderRequest(List<Long> ids) {
}
