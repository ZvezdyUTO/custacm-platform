package top.naccl.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.naccl.constant.RedisKeyConstants;
import top.naccl.constant.SiteSettingConstants;
import top.naccl.entity.SiteSetting;
import top.naccl.exception.PersistenceException;
import top.naccl.mapper.SiteSettingMapper;
import top.naccl.service.RedisService;
import top.naccl.service.SiteSettingService;
import top.naccl.util.JacksonUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
	public Map<String, List<SiteSetting>> getList() {
		List<SiteSetting> siteSettings = siteSettingMapper.getList();
		List<SiteSetting> type1 = new ArrayList<>();
		List<SiteSetting> type2 = new ArrayList<>();
		List<SiteSetting> type3 = new ArrayList<>();
		for (SiteSetting s : siteSettings) {
			switch (s.getType()) {
				case 1:
					type1.add(s);
					break;
				case 2:
					type2.add(s);
					break;
				case 3:
					type3.add(s);
					break;
				default:
					break;
			}
		}
		Map<String, List<SiteSetting>> map = new HashMap<>(8);
		map.put("type1", type1);
		map.put("type2", type2);
		map.put("type3", type3);
		return map;
	}

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

	@Override
	public String getWebTitleSuffix() {
		return siteSettingMapper.getWebTitleSuffix();
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void updateSiteSetting(List<LinkedHashMap> siteSettings, List<Integer> deleteIds) {
		for (Integer id : deleteIds) {
			//删除
			deleteOneSiteSettingById(id);
		}
		for (LinkedHashMap s : siteSettings) {
			SiteSetting siteSetting = JacksonUtils.convertValue(s, SiteSetting.class);
			if (siteSetting.getId() != null) {
				//修改
				updateOneSiteSetting(siteSetting);
			} else {
				//添加
				saveOneSiteSetting(siteSetting);
			}
		}
		deleteSiteInfoRedisCache();
	}

	public void saveOneSiteSetting(SiteSetting siteSetting) {
		if (siteSettingMapper.saveSiteSetting(siteSetting) != 1) {
			throw new PersistenceException("配置添加失败");
		}
	}

	public void updateOneSiteSetting(SiteSetting siteSetting) {
		if (siteSettingMapper.updateSiteSetting(siteSetting) != 1) {
			throw new PersistenceException("配置修改失败");
		}
	}

	public void deleteOneSiteSettingById(Integer id) {
		if (siteSettingMapper.deleteSiteSettingById(id) != 1) {
			throw new PersistenceException("配置删除失败");
		}
	}

	/**
	 * 删除站点信息缓存
	 */
	private void deleteSiteInfoRedisCache() {
		redisService.deleteCacheByKey(RedisKeyConstants.SITE_INFO_MAP);
	}
}
