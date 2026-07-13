package top.naccl.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.naccl.mapper.BlogMapper;
import top.naccl.model.vo.SearchBlog;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * @author huangbingrui.awa
 */
@ExtendWith(MockitoExtension.class)
class BlogSearchServiceTest {
	@Mock
	private BlogMapper blogMapper;

	private BlogServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new BlogServiceImpl();
		service.blogMapper = blogMapper;
	}

	@Test
	void returnsArticleDescriptionsForTitleMatches() {
		SearchBlog titleMatch = searchBlog(2L, "Needle 标题", "用于搜索建议的文章简介");
		SearchBlog emptyDescription = searchBlog(3L, "Needle 空简介", null);
		when(blogMapper.getSearchBlogListByQueryAndIsPublished("needle", false))
				.thenReturn(List.of(titleMatch, emptyDescription));

		List<SearchBlog> results = service.getSearchBlogListByQueryAndIsPublished("needle", false);

		assertEquals("用于搜索建议的文章简介", results.get(0).getDescription());
		assertEquals(null, results.get(1).getDescription());
	}

	@Test
	void mapperLimitsAnonymousSearchButLetsAuthenticatedReadsIncludeInternalArticles() throws IOException {
		String mapper = new String(getClass().getResourceAsStream("/mapper/BlogMapper.xml").readAllBytes(), StandardCharsets.UTF_8);

		assertTrue(mapper.contains("is_published=true"));
		assertTrue(mapper.contains("is_internal=false"));
		assertTrue(mapper.contains("<if test=\"includeInternal == false\">"));
		assertTrue(mapper.contains("and title like #{queryPattern}"));
		assertTrue(mapper.contains("select id, title, description"));
		assertTrue(!mapper.contains("title like #{queryPattern} or content like #{queryPattern}"));
		assertTrue(mapper.contains("order by update_time desc, id desc"));
		assertTrue(mapper.contains("limit 10"));
	}

	private SearchBlog searchBlog(Long id, String title, String description) {
		SearchBlog searchBlog = new SearchBlog();
		searchBlog.setId(id);
		searchBlog.setTitle(title);
		searchBlog.setDescription(description);
		return searchBlog;
	}
}
