package top.naccl.model.vo;

import lombok.Getter;
import top.naccl.entity.User;

import java.util.List;

/**
 * @author huangbingrui.awa
 */
@Getter
public class PlayerProfile {
	private final String username;
	private final String nickname;
	private final String avatar;
	private final String avatarOriginalUrl;
	private final String signature;
	private final String role;
	private final List<ProfileLinkResponse> links;

	public PlayerProfile(User user) {
		this(user, List.of(), user.getAvatar());
	}

	public PlayerProfile(User user, List<ProfileLinkResponse> links) {
		this(user, links, user.getAvatar());
	}

	public PlayerProfile(User user, List<ProfileLinkResponse> links, String avatarOriginalUrl) {
		this.username = user.getUsername();
		this.nickname = user.getNickname();
		this.avatar = user.getAvatar();
		this.avatarOriginalUrl = avatarOriginalUrl;
		this.signature = user.getSignature();
		this.role = user.getRole();
		this.links = List.copyOf(links);
	}
}
