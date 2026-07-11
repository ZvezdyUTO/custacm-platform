package top.naccl.model.vo;

import top.naccl.entity.UserProfileLink;

/**
 * @author huangbingrui.awa
 */
public record ProfileLinkResponse(Long id, String label, String url, Integer sortOrder) {
	public ProfileLinkResponse(UserProfileLink link) {
		this(link.getId(), link.getLabel(), link.getUrl(), link.getSortOrder());
	}
}
