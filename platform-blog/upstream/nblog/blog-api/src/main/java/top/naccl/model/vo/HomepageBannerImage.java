package top.naccl.model.vo;

/**
 * 首页横幅图片及其从左到右的展示顺序。
 *
 * @author huangbingrui.awa
 */
public record HomepageBannerImage(Long id, String imageUrl, Integer sortOrder) {
}
