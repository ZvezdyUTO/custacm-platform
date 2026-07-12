package top.naccl.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import top.naccl.constant.RedisKeyConstants;
import top.naccl.entity.ImageAsset;
import top.naccl.entity.User;
import top.naccl.exception.BadRequestException;
import top.naccl.exception.NotFoundException;
import top.naccl.mapper.UserMapper;
import top.naccl.mapper.UserProfileLinkMapper;
import top.naccl.model.vo.PlayerProfile;
import top.naccl.model.vo.ProfileLinkResponse;

/**
 * @author huangbingrui.awa
 */
@Service
public class PlayerAvatarService {
	private final UserMapper userMapper;
	private final UserProfileLinkMapper linkMapper;
	private final RedisService redisService;
	private final ImageAssetService imageAssetService;

	public PlayerAvatarService(UserMapper userMapper, UserProfileLinkMapper linkMapper, RedisService redisService,
			ImageAssetService imageAssetService) {
		this.userMapper = userMapper;
		this.linkMapper = linkMapper;
		this.redisService = redisService;
		this.imageAssetService = imageAssetService;
	}

	@Transactional
	public PlayerProfile updateAvatar(String username, MultipartFile file) {
		User user = userMapper.findByUsername(username);
		if (user == null) {
			throw new NotFoundException("用户不存在");
		}
		ImageAsset previous = imageAssetService.findById(user.getAvatarAssetId());
		ImageAsset replacement = imageAssetService.storeAvatar(user.getId(), file);
		if (userMapper.updateAvatarByUsername(username, replacement.getThumbnailUrl(), replacement.getId()) != 1) {
			throw new BadRequestException("头像更新失败");
		}
		imageAssetService.replaceAvatar(previous, replacement);
		redisService.deleteCacheByKey(RedisKeyConstants.HOME_BLOG_INFO_LIST);
		user.setAvatar(replacement.getThumbnailUrl());
		user.setAvatarAssetId(replacement.getId());
		return new PlayerProfile(user, linkMapper.findByUserId(user.getId()).stream()
				.map(ProfileLinkResponse::new)
				.toList(), replacement.getOriginalUrl());
	}
}
