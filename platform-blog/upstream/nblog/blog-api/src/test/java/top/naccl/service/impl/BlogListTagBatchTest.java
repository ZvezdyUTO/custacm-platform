package top.naccl.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.naccl.constant.RedisKeyConstants;
import top.naccl.entity.Tag;
import top.naccl.exception.BadRequestException;
import top.naccl.mapper.BlogMapper;
import top.naccl.model.vo.BlogInfo;
import top.naccl.service.TagService;
import top.naccl.service.RedisService;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * @author huangbingrui.awa
 */
@ExtendWith(MockitoExtension.class)
class BlogListTagBatchTest {
    @Mock
    private BlogMapper blogMapper;
    @Mock
    private TagService tagService;
    @Mock
    private RedisService redisService;

    private BlogServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BlogServiceImpl();
        service.blogMapper = blogMapper;
        service.tagService = tagService;
        service.redisService = redisService;
    }

    @AfterEach
    void clearPageHelperState() {
        PageHelper.clearPage();
    }

    @Test
    void usesSixArticlesPerPublicCatalogPage() {
        assertThat(BlogServiceImpl.PAGE_SIZE).isEqualTo(6);
        assertThat(RedisKeyConstants.HOME_BLOG_INFO_LIST).endsWith(":v3");
    }

    @Test
    void loadsAllTagsForTheCurrentPageInOneBatch() {
        BlogInfo first = blogInfo(11L, "first");
        BlogInfo second = blogInfo(12L, "second");
        Tag tag = new Tag();
        tag.setId(7L);
        tag.setName("算法");
        tag.setColor("#112233");
        when(blogMapper.getBlogInfoListByCategoryNameAndIsPublished("题解", false))
                .thenReturn(List.of(first, second));
        when(tagService.getTagListsByBlogIds(List.of(11L, 12L)))
                .thenReturn(Map.of(11L, List.of(tag)));

        var result = service.getBlogInfoListByCategoryNameAndIsPublished("题解", 1, false);

        assertThat(result.getList().get(0).getTags()).containsExactly(tag);
        assertThat(result.getList().get(1).getTags()).isEmpty();
        verify(tagService).getTagListsByBlogIds(List.of(11L, 12L));
    }

    @Test
    void rejectsInvalidPagesBeforeReadingCachesOrTheDatabase() {
        assertThatThrownBy(() -> service.getBlogInfoListByIsPublished(0, false))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.getBlogInfoListByIsPublished(-1, true))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.getBlogInfoListByCategoryNameAndIsPublished("题解", -1, false))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.getBlogInfoListByTagNameAndIsPublished("算法", null, false))
                .isInstanceOf(BadRequestException.class);

        verifyNoInteractions(blogMapper, tagService, redisService);
    }

    @Test
    void doesNotCacheTheSameLastPageUnderArbitrarilyLargePageNumbers() {
        Page<BlogInfo> rows = new Page<>(2, BlogServiceImpl.PAGE_SIZE);
        rows.setTotal(7);
        rows.add(blogInfo(42L, "last article"));
        when(blogMapper.getBlogInfoListByIsPublished(false)).thenReturn(rows);

        var result = service.getBlogInfoListByIsPublished(1_000_000, false);

        assertThat(result.getTotalPage()).isEqualTo(2);
        assertThat(result.getList()).hasSize(1);
        verify(redisService, never()).saveKVToHash(any(), any(), any());
    }

    @Test
    void cachesTheFirstPageEvenWhenTheSiteHasNoArticles() {
        Page<BlogInfo> rows = new Page<>(1, BlogServiceImpl.PAGE_SIZE);
        rows.setTotal(0);
        when(blogMapper.getBlogInfoListByIsPublished(false)).thenReturn(rows);

        var result = service.getBlogInfoListByIsPublished(1, false);

        assertThat(result.getList()).isEmpty();
        verify(redisService).saveKVToHash(RedisKeyConstants.HOME_BLOG_INFO_LIST, 1, result);
    }

    @Test
    void cachesAnExistingPublicPageAndPreservesItsPageCount() {
        Page<BlogInfo> rows = new Page<>(1, BlogServiceImpl.PAGE_SIZE);
        rows.setTotal(12);
        rows.add(blogInfo(42L, "first article"));
        when(blogMapper.getBlogInfoListByIsPublished(false)).thenReturn(rows);

        var result = service.getBlogInfoListByIsPublished(1, false);

        assertThat(result.getTotalPage()).isEqualTo(2);
        verify(redisService).saveKVToHash(RedisKeyConstants.HOME_BLOG_INFO_LIST, 1, result);
    }

    private static BlogInfo blogInfo(Long id, String description) {
        BlogInfo blogInfo = new BlogInfo();
        blogInfo.setId(id);
        blogInfo.setDescription(description);
        return blogInfo;
    }
}
