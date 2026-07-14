package top.naccl.model.dto;

import java.util.List;

/**
 * 首页精选组创建或替换请求。
 *
 * @author huangbingrui.awa
 */
public record HomepageFeaturedGroupUpsertRequest(String title, List<Long> articleIds) {
}
