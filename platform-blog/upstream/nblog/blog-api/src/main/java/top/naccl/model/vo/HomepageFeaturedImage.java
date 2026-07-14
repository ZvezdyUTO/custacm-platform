package top.naccl.model.vo;

/**
 * 首页精选图片及其横向展示顺序。
 *
 * @author huangbingrui.awa
 */
public record HomepageFeaturedImage(Long id, String imageUrl, String thumbnailUrl, Integer sortOrder) {
}
