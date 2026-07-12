package top.naccl.model.vo;

import lombok.Getter;
import top.naccl.entity.User;

import java.util.List;

/**
 * @author huangbingrui.awa
 */
@Getter
public class PublicProfile {
	private final String username;
	private final String nickname;
	private final String email;
	private final String avatar;
	private final String signature;
	private final List<ProfileLinkResponse> links;

	public PublicProfile(User user, List<ProfileLinkResponse> links, String avatar) {
		this.username = user.getUsername();
		this.nickname = user.getNickname();
		this.email = user.getEmail();
		this.avatar = avatar;
		this.signature = user.getSignature();
		this.links = List.copyOf(links);
	}
}
