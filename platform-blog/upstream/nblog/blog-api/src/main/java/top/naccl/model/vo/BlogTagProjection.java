package top.naccl.model.vo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import top.naccl.entity.Tag;

/**
 * 批量加载文章标签时使用的只读投影。
 *
 * @author huangbingrui.awa
 */
@Getter
@Setter
@NoArgsConstructor
public class BlogTagProjection {
    private Long blogId;
    private Long tagId;
    private String tagName;
    private String color;

    public Tag toTag() {
        Tag tag = new Tag();
        tag.setId(tagId);
        tag.setName(tagName);
        tag.setColor(color);
        return tag;
    }
}
