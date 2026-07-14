package top.naccl.model.vo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;

/**
 * @Description: 随机博客
 * @Author: Naccl
 * @Date: 2020-08-17
 */
@NoArgsConstructor
@Getter
@Setter
@ToString
public class RandomBlog {
	private Long id;
	private String title;//文章标题
	private String description;//文章简介
	private String firstPicture;//文章首图，用于随机文章展示
	private Date createTime;//创建时间
	private String categoryName;//文章分类
	private Boolean top;//是否由管理员置顶
}
