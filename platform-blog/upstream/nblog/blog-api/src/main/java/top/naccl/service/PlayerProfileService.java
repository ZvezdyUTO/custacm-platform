package top.naccl.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.naccl.constant.RedisKeyConstants;
import top.naccl.entity.User;
import top.naccl.entity.UserProfileLink;
import top.naccl.exception.BadRequestException;
import top.naccl.exception.NotFoundException;
import top.naccl.mapper.UserMapper;
import top.naccl.mapper.UserProfileLinkMapper;
import top.naccl.model.dto.PlayerProfileUpdateRequest;
import top.naccl.model.dto.ProfileLinkInput;
import top.naccl.model.dto.ProfileLinksReplaceRequest;
import top.naccl.model.vo.PlayerProfile;
import top.naccl.model.vo.ProfileLinkResponse;
import top.naccl.model.vo.PublicProfile;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * @author huangbingrui.awa
 */
@Service
public class PlayerProfileService {
	static final int MAX_NICKNAME_LENGTH = 30;
	static final int MAX_SIGNATURE_LENGTH = 160;
	static final int MAX_LINK_COUNT = 8;
	static final int MAX_LINK_LABEL_LENGTH = 30;
	static final int MAX_LINK_URL_LENGTH = 2048;

	private final UserMapper userMapper;
	private final UserProfileLinkMapper linkMapper;
	private final RedisService redisService;
	private final ImageAssetService imageAssetService;
	private final CompetitionService competitionService;

	public PlayerProfileService(UserMapper userMapper, UserProfileLinkMapper linkMapper, RedisService redisService,
			ImageAssetService imageAssetService, CompetitionService competitionService) {
		this.userMapper = userMapper;
		this.linkMapper = linkMapper;
		this.redisService = redisService;
		this.imageAssetService = imageAssetService;
		this.competitionService = competitionService;
	}

	public PlayerProfile get(String username) {
		User user = requireUser(username);
		return profile(user);
	}

	public PublicProfile getPublic(String username) {
		User user = requireUser(username);
		List<ProfileLinkResponse> links = links(user);
		var avatar = imageAssetService.response(user.getAvatarAssetId());
		return new PublicProfile(user, links, avatar == null ? user.getAvatar() : avatar.originalUrl(),
				competitionService.publicAchievements(user.getUsername()));
	}

	@Transactional
	public PlayerProfile update(String username, PlayerProfileUpdateRequest request) {
		if (request == null || (request.nickname() == null && request.signature() == null)) {
			throw new BadRequestException("昵称和个性签名至少需要填写一项");
		}
		User user = requireUser(username);
		String nickname = request.nickname() == null ? user.getNickname() : normalizeNickname(request.nickname());
		String signature = request.signature() == null ? emptyIfNull(user.getSignature()) : normalizeSignature(request.signature());
		if (userMapper.updateProfileByUsername(username, nickname, signature) != 1) {
			throw new IllegalStateException("修改个人资料失败");
		}
		if (!nickname.equals(user.getNickname())) {
			redisService.deleteCacheByKey(RedisKeyConstants.HOME_BLOG_INFO_LIST);
		}
		user.setNickname(nickname);
		user.setSignature(signature);
		return profile(user);
	}

	@Transactional
	public PlayerProfile replaceLinks(String username, ProfileLinksReplaceRequest request) {
		if (request == null || request.links() == null) {
			throw new BadRequestException("友情链接列表不能为空");
		}
		if (request.links().size() > MAX_LINK_COUNT) {
			throw new BadRequestException("友情链接最多添加 8 条");
		}
		List<NormalizedLink> links = normalizeLinks(request.links());
		User user = requireUser(username);
		linkMapper.deleteByUserId(user.getId());
		for (int index = 0; index < links.size(); index++) {
			NormalizedLink source = links.get(index);
			UserProfileLink link = new UserProfileLink();
			link.setUserId(user.getId());
			link.setLabel(source.label());
			link.setUrl(source.url());
			link.setSortOrder(index);
			if (linkMapper.insert(link) != 1) {
				throw new IllegalStateException("保存友情链接失败");
			}
		}
		return profile(user);
	}

	private PlayerProfile profile(User user) {
		List<ProfileLinkResponse> links = links(user);
		var avatar = imageAssetService.response(user.getAvatarAssetId());
		return new PlayerProfile(user, links, avatar == null ? user.getAvatar() : avatar.originalUrl(),
				competitionService.achievements(user.getUsername()));
	}

	private List<ProfileLinkResponse> links(User user) {
		return linkMapper.findByUserId(user.getId()).stream()
				.map(ProfileLinkResponse::new)
				.toList();
	}

	private User requireUser(String username) {
		User user = userMapper.findByUsername(username);
		if (user == null) {
			throw new NotFoundException("用户不存在");
		}
		return user;
	}

	private static String normalizeNickname(String value) {
		String nickname = value.trim();
		if (nickname.isEmpty() || nickname.length() > MAX_NICKNAME_LENGTH) {
			throw new BadRequestException("昵称长度应为 1 到 30 个字符");
		}
		return nickname;
	}

	private static String normalizeSignature(String value) {
		String signature = value.trim();
		if (signature.length() > MAX_SIGNATURE_LENGTH) {
			throw new BadRequestException("个性签名不能超过 160 个字符");
		}
		return signature;
	}

	private static List<NormalizedLink> normalizeLinks(List<ProfileLinkInput> inputs) {
		Set<String> urls = new HashSet<>();
		return inputs.stream().map(input -> {
			if (input == null) {
				throw new BadRequestException("友情链接内容不能为空");
			}
			String label = input.label() == null ? "" : input.label().trim();
			if (label.isEmpty() || label.length() > MAX_LINK_LABEL_LENGTH) {
				throw new BadRequestException("友情链接名称长度应为 1 到 30 个字符");
			}
			String url = normalizeUrl(input.url());
			if (!urls.add(url)) {
				throw new BadRequestException("友情链接地址不能重复");
			}
			return new NormalizedLink(label, url);
		}).toList();
	}

	private static String normalizeUrl(String value) {
		String url = value == null ? "" : value.trim();
		if (url.isEmpty() || url.length() > MAX_LINK_URL_LENGTH) {
			throw new BadRequestException("友情链接地址长度应为 1 到 2048 个字符");
		}
		try {
			URI uri = new URI(url);
			String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
			if (!("http".equals(scheme) || "https".equals(scheme)) || uri.getRawAuthority() == null
					|| uri.getRawAuthority().isBlank()) {
				throw new BadRequestException("友情链接必须是完整的 HTTP 或 HTTPS 地址");
			}
			return uri.toString();
		} catch (URISyntaxException exception) {
			throw new BadRequestException("友情链接地址格式不正确");
		}
	}

	private static String emptyIfNull(String value) {
		return value == null ? "" : value;
	}

	private record NormalizedLink(String label, String url) {
	}
}
