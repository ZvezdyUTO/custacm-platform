package top.naccl.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

/**
 * @author huangbingrui.awa
 */
@NoArgsConstructor
@Getter
@Setter
public class UserProfileLink {
	private Long id;
	private Long userId;
	private String label;
	private String url;
	private Integer sortOrder;
	private Date createTime;
	private Date updateTime;
}
