package top.naccl.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

/**
 * 托管图片的物理文件与生命周期元数据。
 *
 * @author huangbingrui.awa
 */
@Getter
@Setter
@NoArgsConstructor
public class ImageAsset {
	public enum Purpose {
		ARTICLE_COVER,
		ARTICLE_CONTENT,
		AVATAR
	}

	public enum Status {
		TEMP,
		ACTIVE,
		DELETING
	}

	private Long id;
	private String publicId;
	private Long ownerUserId;
	private String purpose;
	private String originalPath;
	private String thumbnailPath;
	private String originalUrl;
	private String thumbnailUrl;
	private String mimeType;
	private Integer width;
	private Integer height;
	private Long originalBytes;
	private Long thumbnailBytes;
	private String status;
	private Date createTime;
	private Date updateTime;
	private Long blogId;
	private String referenceRole;
}
