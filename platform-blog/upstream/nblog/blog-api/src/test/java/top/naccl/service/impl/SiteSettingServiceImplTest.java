package top.naccl.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.naccl.constant.RedisKeyConstants;
import top.naccl.entity.SiteSetting;
import top.naccl.mapper.SiteSettingMapper;
import top.naccl.service.RedisService;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author huangbingrui.awa
 */
@ExtendWith(MockitoExtension.class)
class SiteSettingServiceImplTest {
	@Mock
	private SiteSettingMapper siteSettingMapper;
	@Mock
	private RedisService redisService;

	private SiteSettingServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new SiteSettingServiceImpl();
		service.siteSettingMapper = siteSettingMapper;
		service.redisService = redisService;
	}

	@Test
	void returnsOnlyPublicSettingsStillConsumedByTheFrontend() {
		when(redisService.getMapByValue(RedisKeyConstants.SITE_INFO_MAP)).thenReturn(null);
		when(siteSettingMapper.getPublicSiteInfoList()).thenReturn(List.of(
				setting("reward", "/img/reward.jpg"),
				setting("commentAdminFlag", "管理员"),
				setting("avatar", "/img/avatar.jpg"),
				setting("name", "Naccl")
		));

		Map<String, Object> result = service.getSiteInfo();

		assertEquals(Map.of("reward", "/img/reward.jpg", "commentAdminFlag", "管理员"), result.get("siteInfo"));
		assertEquals(Map.of("avatar", "/img/avatar.jpg", "name", "Naccl"), result.get("introduction"));
		assertEquals(2, result.size());
		verify(redisService).saveMapToValue(RedisKeyConstants.SITE_INFO_MAP, result);
	}

	private SiteSetting setting(String name, String value) {
		SiteSetting setting = new SiteSetting();
		setting.setNameEn(name);
		setting.setValue(value);
		return setting;
	}
}
