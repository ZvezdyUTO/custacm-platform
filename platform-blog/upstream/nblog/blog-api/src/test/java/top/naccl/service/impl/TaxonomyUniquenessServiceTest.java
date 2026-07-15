package top.naccl.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import top.naccl.entity.Category;
import top.naccl.entity.Tag;
import top.naccl.exception.ConflictException;
import top.naccl.exception.NotFoundException;
import top.naccl.mapper.BlogMapper;
import top.naccl.mapper.CategoryMapper;
import top.naccl.mapper.TagMapper;
import top.naccl.service.RedisService;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * @author huangbingrui.awa
 */
class TaxonomyUniquenessServiceTest {
	@Test
	void categoryDuplicateKeyBecomesResourceConflict() {
		CategoryMapper mapper = mock(CategoryMapper.class);
		RedisService redisService = mock(RedisService.class);
		Category category = new Category();
		category.setName("算法");
		when(mapper.saveCategory(category)).thenThrow(new DuplicateKeyException("duplicate category"));
		CategoryServiceImpl service = new CategoryServiceImpl(mapper, redisService);

		assertThatThrownBy(() -> service.saveCategory(category))
				.isInstanceOf(ConflictException.class)
				.hasMessage("该分类已存在")
				.hasCauseInstanceOf(DuplicateKeyException.class);
		verifyNoInteractions(redisService);
	}

	@Test
	void tagDuplicateKeyBecomesResourceConflict() {
		TagMapper mapper = mock(TagMapper.class);
		RedisService redisService = mock(RedisService.class);
		Tag tag = new Tag();
		tag.setName("Java");
		when(mapper.saveTag(tag)).thenThrow(new DuplicateKeyException("duplicate tag"));
		TagServiceImpl service = new TagServiceImpl(mapper, redisService);

		assertThatThrownBy(() -> service.saveTag(tag))
				.isInstanceOf(ConflictException.class)
				.hasMessage("该标签已存在")
				.hasCauseInstanceOf(DuplicateKeyException.class);
		verifyNoInteractions(redisService);
	}

	@Test
	void categoryRenameDuplicateKeyBecomesResourceConflict() {
		CategoryMapper mapper = mock(CategoryMapper.class);
		RedisService redisService = mock(RedisService.class);
		Category category = new Category();
		category.setId(2L);
		category.setName("算法");
		when(mapper.updateCategory(category)).thenThrow(new DuplicateKeyException("duplicate category"));
		CategoryServiceImpl service = new CategoryServiceImpl(mapper, redisService);

		assertThatThrownBy(() -> service.updateCategory(category))
				.isInstanceOf(ConflictException.class)
				.hasMessage("该分类已存在")
				.hasCauseInstanceOf(DuplicateKeyException.class);
		verifyNoInteractions(redisService);
	}

	@Test
	void missingCategoryUpdateBecomesNotFound() {
		CategoryMapper mapper = mock(CategoryMapper.class);
		RedisService redisService = mock(RedisService.class);
		Category category = new Category();
		category.setId(99L);
		category.setName("不存在");
		when(mapper.updateCategory(category)).thenReturn(0);
		CategoryServiceImpl service = new CategoryServiceImpl(mapper, redisService);

		assertThatThrownBy(() -> service.updateCategory(category))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("分类不存在");
		verifyNoInteractions(redisService);
	}

	@Test
	void missingCategoryDeleteBecomesNotFound() {
		CategoryMapper mapper = mock(CategoryMapper.class);
		RedisService redisService = mock(RedisService.class);
		when(mapper.deleteCategoryById(99L)).thenReturn(0);
		CategoryServiceImpl service = new CategoryServiceImpl(mapper, redisService);

		assertThatThrownBy(() -> service.deleteCategoryById(99L))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("分类不存在");
		verifyNoInteractions(redisService);
	}

	@Test
	void referencedCategoryDeleteBecomesResourceConflict() {
		CategoryMapper mapper = mock(CategoryMapper.class);
		RedisService redisService = mock(RedisService.class);
		when(mapper.deleteCategoryById(3L))
				.thenThrow(new DataIntegrityViolationException("fk_blog_category"));
		CategoryServiceImpl service = new CategoryServiceImpl(mapper, redisService);

		assertThatThrownBy(() -> service.deleteCategoryById(3L))
				.isInstanceOf(ConflictException.class)
				.hasMessage("已有博客与此分类关联，不可删除")
				.hasCauseInstanceOf(DataIntegrityViolationException.class);
		verifyNoInteractions(redisService);
	}

	@Test
	void missingTagDeleteBecomesNotFound() {
		TagMapper mapper = mock(TagMapper.class);
		RedisService redisService = mock(RedisService.class);
		when(mapper.deleteTagById(99L)).thenReturn(0);
		TagServiceImpl service = new TagServiceImpl(mapper, redisService);

		assertThatThrownBy(() -> service.deleteTagById(99L))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("标签不存在");
		verifyNoInteractions(redisService);
	}

	@Test
	void referencedTagDeleteBecomesResourceConflict() {
		TagMapper mapper = mock(TagMapper.class);
		RedisService redisService = mock(RedisService.class);
		when(mapper.deleteTagById(3L))
				.thenThrow(new DataIntegrityViolationException("fk_blog_tag_tag"));
		TagServiceImpl service = new TagServiceImpl(mapper, redisService);

		assertThatThrownBy(() -> service.deleteTagById(3L))
				.isInstanceOf(ConflictException.class)
				.hasMessage("已有博客与此标签关联，不可删除")
				.hasCauseInstanceOf(DataIntegrityViolationException.class);
		verifyNoInteractions(redisService);
	}

	@Test
	void duplicateBlogTagPairBecomesResourceConflict() {
		BlogMapper mapper = mock(BlogMapper.class);
		when(mapper.saveBlogTag(12L, 7L))
				.thenThrow(new DuplicateKeyException("duplicate blog tag pair"));
		BlogServiceImpl service = new BlogServiceImpl();
		service.blogMapper = mapper;

		assertThatThrownBy(() -> service.saveBlogTag(12L, 7L))
				.isInstanceOf(ConflictException.class)
				.hasMessage("文章标签已发生变化，请刷新后重试")
				.hasCauseInstanceOf(DuplicateKeyException.class);
	}

	@Test
	void staleBlogCategoryReferenceBecomesResourceConflictOnCreateAndUpdate() {
		BlogMapper mapper = mock(BlogMapper.class);
		RedisService redisService = mock(RedisService.class);
		top.naccl.model.dto.Blog blog = new top.naccl.model.dto.Blog();
		when(mapper.saveBlog(blog)).thenThrow(new DataIntegrityViolationException("fk_blog_category"));
		when(mapper.updateBlog(blog)).thenThrow(new DataIntegrityViolationException("fk_blog_category"));
		BlogServiceImpl service = new BlogServiceImpl();
		service.blogMapper = mapper;
		service.redisService = redisService;

		assertThatThrownBy(() -> service.saveBlog(blog))
				.isInstanceOf(ConflictException.class)
				.hasMessage("文章分类已发生变化，请刷新后重试")
				.hasCauseInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> service.updateBlog(blog))
				.isInstanceOf(ConflictException.class)
				.hasMessage("文章分类已发生变化，请刷新后重试")
				.hasCauseInstanceOf(DataIntegrityViolationException.class);
		verifyNoInteractions(redisService);
	}
}
