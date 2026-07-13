package top.naccl.service.impl;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import top.naccl.mapper.BlogMapper;
import top.naccl.mapper.CommentMapper;
import top.naccl.mapper.UserMapper;
import top.naccl.service.ArticleRecycleBinService;
import top.naccl.service.ImageAssetService;
import top.naccl.service.RedisService;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

/**
 * @author huangbingrui.awa
 */
class BlogDeletionTransactionTest {
    private AnnotationConfigApplicationContext context;
    private JdbcTemplate jdbcTemplate;
    private BlogMapper blogMapper;
    private CommentMapper commentMapper;
    private ArticleRecycleBinService recycleBinService;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext(TestConfig.class);
        jdbcTemplate = context.getBean(JdbcTemplate.class);
        blogMapper = context.getBean(BlogMapper.class);
        commentMapper = context.getBean(CommentMapper.class);
        recycleBinService = context.getBean(ArticleRecycleBinService.class);

        jdbcTemplate.execute("create table blog_record (id bigint primary key)");
        jdbcTemplate.execute("create table blog_tag_record (blog_id bigint not null)");
        jdbcTemplate.execute("create table comment_record (blog_id bigint not null)");
        jdbcTemplate.update("insert into blog_record (id) values (42)");
        jdbcTemplate.update("insert into blog_tag_record (blog_id) values (42)");
        jdbcTemplate.update("insert into comment_record (blog_id) values (42)");

        when(commentMapper.deleteCommentsByBlogId(anyLong())).thenAnswer(invocation ->
                jdbcTemplate.update("delete from comment_record where blog_id = ?",
                        new Object[]{invocation.getArgument(0)}));
        when(blogMapper.deleteBlogTagByBlogId(anyLong())).thenAnswer(invocation ->
                jdbcTemplate.update("delete from blog_tag_record where blog_id = ?",
                        new Object[]{invocation.getArgument(0)}));
		when(blogMapper.findExpiredRecycleBinBlogIds(org.mockito.ArgumentMatchers.any()))
				.thenReturn(List.of(42L));
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void rollsBackCommentsAndTagsWhenTheFinalBlogDeleteFails() {
        when(blogMapper.deleteBlogById(42L)).thenAnswer(invocation -> {
            jdbcTemplate.update("delete from blog_record where id = 42");
            throw new IllegalStateException("simulated database failure");
        });

        assertThatThrownBy(() -> recycleBinService.purgeExpired())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("simulated database failure");

        assertThat(count("blog_record")).isEqualTo(1);
        assertThat(count("blog_tag_record")).isEqualTo(1);
        assertThat(count("comment_record")).isEqualTo(1);
    }

    private int count(String table) {
        return jdbcTemplate.queryForObject("select count(*) from " + table, Integer.class);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    static class TestConfig {
        @Bean
        DataSource dataSource() {
            JdbcDataSource dataSource = new JdbcDataSource();
            dataSource.setURL("jdbc:h2:mem:blog-delete;MODE=MySQL;DB_CLOSE_DELAY=-1");
            return dataSource;
        }

        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        BlogMapper blogMapper() {
            return mock(BlogMapper.class);
        }

        @Bean
        CommentMapper commentMapper() {
            return mock(CommentMapper.class);
        }

        @Bean
		UserMapper userMapper() {
			return mock(UserMapper.class);
        }

        @Bean
        RedisService redisService() {
            return mock(RedisService.class);
        }

        @Bean
        ImageAssetService imageAssetService() {
            return mock(ImageAssetService.class);
        }

        @Bean
		ArticleRecycleBinService recycleBinService(BlogMapper blogMapper, CommentMapper commentMapper,
				UserMapper userMapper, RedisService redisService, ImageAssetService imageAssetService) {
			return new ArticleRecycleBinService(blogMapper, commentMapper, userMapper,
					imageAssetService, redisService);
		}
    }
}
