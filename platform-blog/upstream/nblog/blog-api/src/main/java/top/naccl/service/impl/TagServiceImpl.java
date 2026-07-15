package top.naccl.service.impl;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.naccl.constant.RedisKeyConstants;
import top.naccl.constant.TaxonomyColorPalette;
import top.naccl.entity.Tag;
import top.naccl.exception.ConflictException;
import top.naccl.exception.NotFoundException;
import top.naccl.exception.PersistenceException;
import top.naccl.mapper.TagMapper;
import top.naccl.model.vo.BlogTagProjection;
import top.naccl.service.RedisService;
import top.naccl.service.TagService;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description: 博客标签业务层实现
 * @Author: Naccl
 * @Date: 2020-07-30
 */
@Service
public class TagServiceImpl implements TagService {
	private final TagMapper tagMapper;
	private final RedisService redisService;

	public TagServiceImpl(TagMapper tagMapper, RedisService redisService) {
		this.tagMapper = tagMapper;
		this.redisService = redisService;
	}

	@Override
	public List<Tag> getTagList() {
		return tagMapper.getTagList();
	}

	@Override
	public List<Tag> getTagListNotId() {
		String redisKey = RedisKeyConstants.TAG_CLOUD_LIST;
		List<Tag> tagListFromRedis = redisService.getListByValue(redisKey);
		if (tagListFromRedis != null) {
			return tagListFromRedis;
		}
		List<Tag> tagList = tagMapper.getTagListNotId();
		redisService.saveListToValue(redisKey, tagList);
		return tagList;
	}

	@Override
	public Map<Long, List<Tag>> getTagListsByBlogIds(Collection<Long> blogIds) {
		if (blogIds == null || blogIds.isEmpty()) {
			return Map.of();
		}
		Map<Long, List<Tag>> tagsByBlogId = new LinkedHashMap<>();
		for (BlogTagProjection projection : tagMapper.getTagListByBlogIds(blogIds)) {
			tagsByBlogId.computeIfAbsent(projection.getBlogId(), ignored -> new java.util.ArrayList<>())
					.add(projection.toTag());
		}
		tagsByBlogId.replaceAll((ignored, tags) -> List.copyOf(tags));
		return Map.copyOf(tagsByBlogId);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void saveTag(Tag tag) {
		tag.setColor(TaxonomyColorPalette.randomDark());
		try {
			if (tagMapper.saveTag(tag) != 1) {
				throw new PersistenceException("标签添加失败");
			}
		} catch (DuplicateKeyException exception) {
			throw new ConflictException("该标签已存在", exception);
		}
		redisService.deleteCacheByKey(RedisKeyConstants.TAG_CLOUD_LIST);
	}

	@Override
	public Tag getTagById(Long id) {
		Tag tag = tagMapper.getTagById(id);
		if (tag == null) {
			throw new NotFoundException("标签不存在");
		}
		return tag;
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void deleteTagById(Long id) {
		try {
			if (tagMapper.deleteTagById(id) != 1) {
				throw new NotFoundException("标签不存在");
			}
		} catch (DataIntegrityViolationException exception) {
			throw new ConflictException("已有博客与此标签关联，不可删除", exception);
		}
		redisService.deleteCacheByKey(RedisKeyConstants.TAG_CLOUD_LIST);
	}

}
