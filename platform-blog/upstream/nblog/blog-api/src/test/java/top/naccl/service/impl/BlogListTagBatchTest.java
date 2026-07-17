package top.naccl.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.naccl.constant.RedisKeyConstants;
import top.naccl.entity.Tag;
import top.naccl.mapper.BlogMapper;
import top.naccl.model.vo.BlogInfo;
import top.naccl.service.TagService;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
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

    private BlogServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BlogServiceImpl();
        service.blogMapper = blogMapper;
        service.tagService = tagService;
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

    private static BlogInfo blogInfo(Long id, String description) {
        BlogInfo blogInfo = new BlogInfo();
        blogInfo.setId(id);
        blogInfo.setDescription(description);
        return blogInfo;
    }
}
