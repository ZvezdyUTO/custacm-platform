package top.naccl.service.impl;

import org.junit.jupiter.api.Test;
import top.naccl.mapper.BlogMapper;
import top.naccl.model.vo.RandomBlog;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FeaturedBlogServiceTest {
	@Test
	void returnsThreeOrderedCandidatesWithRenderedDescriptions() {
		BlogMapper mapper = mock(BlogMapper.class);
		BlogServiceImpl service = new BlogServiceImpl();
		service.blogMapper = mapper;
		RandomBlog blog = new RandomBlog();
		blog.setDescription("**训练总结**");
		when(mapper.getFeaturedBlogList(3, false)).thenReturn(List.of(blog));

		List<RandomBlog> result = service.getFeaturedBlogList(false);

		verify(mapper).getFeaturedBlogList(3, false);
		assertTrue(result.getFirst().getDescription().contains("<strong>训练总结</strong>"));
	}
}
