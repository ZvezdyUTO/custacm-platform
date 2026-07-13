package top.naccl.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.naccl.constant.RedisKeyConstants;
import top.naccl.constant.SiteSettingConstants;
import top.naccl.entity.SiteSetting;
import top.naccl.mapper.SiteSettingMapper;
import top.naccl.service.RedisService;
import top.naccl.service.SiteSettingService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description: 站点设置业务层实现
 * @Author: Naccl
 * @Date: 2020-08-09
 */
@Service
public class SiteSettingServiceImpl implements SiteSettingService {
	@Autowired
	SiteSettingMapper siteSettingMapper;
	@Autowired
	RedisService redisService;

	@Override
	public Map<String, Object> getSiteInfo() {
		String redisKey = RedisKeyConstants.SITE_INFO_MAP;
		Map<String, Object> siteInfoMapFromRedis = redisService.getMapByValue(redisKey);
		if (siteInfoMapFromRedis != null) {
			return siteInfoMapFromRedis;
		}
		List<SiteSetting> siteSettings = siteSettingMapper.getPublicSiteInfoList();
		Map<String, Object> siteInfo = new HashMap<>(2);
		Map<String, String> introduction = new HashMap<>(2);
		for (SiteSetting s : siteSettings) {
			switch (s.getNameEn()) {
				case SiteSettingConstants.REWARD:
				case SiteSettingConstants.COMMENT_ADMIN_FLAG:
					siteInfo.put(s.getNameEn(), s.getValue());
					break;
				case SiteSettingConstants.AVATAR:
				case SiteSettingConstants.NAME:
					introduction.put(s.getNameEn(), s.getValue());
					break;
				default:
					break;
			}
		}
		Map<String, Object> map = new HashMap<>(2);
		map.put("introduction", introduction);
		map.put("siteInfo", siteInfo);
		redisService.saveMapToValue(redisKey, map);
		return map;
	}

}
