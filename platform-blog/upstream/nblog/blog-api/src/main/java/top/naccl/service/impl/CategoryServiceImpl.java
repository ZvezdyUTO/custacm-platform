package top.naccl.service.impl;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.naccl.constant.RedisKeyConstants;
import top.naccl.constant.TaxonomyColorPalette;
import top.naccl.entity.Category;
import top.naccl.exception.ConflictException;
import top.naccl.exception.NotFoundException;
import top.naccl.exception.PersistenceException;
import top.naccl.mapper.CategoryMapper;
import top.naccl.service.CategoryService;
import top.naccl.service.RedisService;

import java.util.List;

/**
 * @Description: 博客分类业务层实现
 * @Author: Naccl
 * @Date: 2020-07-29
 */
@Service
public class CategoryServiceImpl implements CategoryService {
	private final CategoryMapper categoryMapper;
	private final RedisService redisService;

	public CategoryServiceImpl(CategoryMapper categoryMapper, RedisService redisService) {
		this.categoryMapper = categoryMapper;
		this.redisService = redisService;
	}

	@Override
	public List<Category> getCategoryList() {
		return categoryMapper.getCategoryList();
	}

	@Override
	public List<Category> getCategoryNameList() {
		String redisKey = RedisKeyConstants.CATEGORY_NAME_LIST;
		List<Category> categoryListFromRedis = redisService.getListByValue(redisKey);
		if (categoryListFromRedis != null) {
			return categoryListFromRedis;
		}
		List<Category> categoryList = categoryMapper.getCategoryNameList();
		redisService.saveListToValue(redisKey, categoryList);
		return categoryList;
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void saveCategory(Category category) {
		category.setColor(TaxonomyColorPalette.normalize(category.getColor()));
		try {
			if (categoryMapper.saveCategory(category) != 1) {
				throw new PersistenceException("分类添加失败");
			}
		} catch (DuplicateKeyException exception) {
			throw new ConflictException("该分类已存在", exception);
		}
		redisService.deleteCacheByKey(RedisKeyConstants.CATEGORY_NAME_LIST);
	}

	@Override
	public Category getCategoryById(Long id) {
		Category category = categoryMapper.getCategoryById(id);
		if (category == null) {
			throw new NotFoundException("分类不存在");
		}
		return category;
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void deleteCategoryById(Long id) {
		try {
			if (categoryMapper.deleteCategoryById(id) != 1) {
				throw new NotFoundException("分类不存在");
			}
		} catch (DataIntegrityViolationException exception) {
			throw new ConflictException("已有博客与此分类关联，不可删除", exception);
		}
		redisService.deleteCacheByKey(RedisKeyConstants.CATEGORY_NAME_LIST);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void updateCategory(Category category) {
		category.setColor(TaxonomyColorPalette.normalize(category.getColor()));
		try {
			if (categoryMapper.updateCategory(category) != 1) {
				throw new NotFoundException("分类不存在");
			}
		} catch (DuplicateKeyException exception) {
			throw new ConflictException("该分类已存在", exception);
		}
		redisService.deleteCacheByKey(RedisKeyConstants.CATEGORY_NAME_LIST);
		//修改了分类名，可能有首页文章关联了分类，也要更新首页缓存
		redisService.deleteCacheByKey(RedisKeyConstants.HOME_BLOG_INFO_LIST);
	}
}
