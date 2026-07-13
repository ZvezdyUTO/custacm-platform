package top.naccl.constant;

/**
 * @Description: Redis key配置
 * @Author: Naccl
 * @Date: 2020-09-27
 */
public class RedisKeyConstants {
	/**
	 * 首页博客简介列表 分页对象key
	 * homeBlogInfoList : {{1,"第一页的缓存"},{2,"第二页的缓存"}}
	 */
	public static final String HOME_BLOG_INFO_LIST = "homeBlogInfoList:v2";
	/**
	 * 分类名列表key
	 */
	public static final String CATEGORY_NAME_LIST = "categoryNameList";
	/**
	 * 标签云列表key
	 */
	public static final String TAG_CLOUD_LIST = "tagCloudList";
	/**
	 * 站点信息key
	 */
	public static final String SITE_INFO_MAP = "publicSiteInfoMap:v2";
	/**
	 * 普通用户文章下载限流 key 前缀
	 */
	public static final String ARTICLE_DOWNLOAD_RATE_LIMIT = "articleDownloadRateLimit:";
	/**
	 * 登录尝试冷却 key 前缀；后缀为规范化 username 的 SHA-256。
	 */
	public static final String LOGIN_ATTEMPT_COOLDOWN = "loginAttemptCooldown:";
}
