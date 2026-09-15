package top.naccl.controller;

import com.github.pagehelper.PageInterceptor;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.web.servlet.MockMvc;
import top.naccl.constant.RedisKeyConstants;
import top.naccl.controller.admin.CategoryAdminController;
import top.naccl.handler.ControllerExceptionHandler;
import top.naccl.mapper.CategoryMapper;
import top.naccl.service.RedisService;
import top.naccl.service.impl.CategoryServiceImpl;

import java.io.InputStream;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class CategoryDescriptionTest {
	private final RedisService redis = mock(RedisService.class);
	private SqlSession session;
	private CategoryServiceImpl service;
	private MockMvc mvc;

	@BeforeEach
	void setUp() throws Exception {
		when(redis.getListByValue(RedisKeyConstants.CATEGORY_NAME_LIST)).thenReturn(null);
		JdbcDataSource dataSource = new JdbcDataSource();
		dataSource.setURL("jdbc:h2:mem:category-description-" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
		JdbcTemplate jdbc = new JdbcTemplate(dataSource);
		jdbc.execute("CREATE TABLE category (id BIGINT AUTO_INCREMENT PRIMARY KEY, category_name VARCHAR(255), color VARCHAR(7))");
		jdbc.update("INSERT INTO category (category_name, color) VALUES ('训练经验', '#8B1E3F')");
		new ResourceDatabasePopulator(new ClassPathResource("db/migration/V052__add_category_description.sql")).execute(dataSource);
		Configuration configuration = new Configuration(new Environment("test", new JdbcTransactionFactory(), dataSource));
		configuration.addInterceptor(new PageInterceptor());
		try (InputStream mapper = getClass().getResourceAsStream("/mapper/CategoryMapper.xml")) {
			new XMLMapperBuilder(mapper, configuration, "mapper/CategoryMapper.xml", configuration.getSqlFragments()).parse();
		}
		session = new SqlSessionFactoryBuilder().build(configuration).openSession(true);
		service = new CategoryServiceImpl(session.getMapper(CategoryMapper.class), redis);
		CategoryController publicController = new CategoryController();
		publicController.categoryService = service;
		mvc = standaloneSetup(new CategoryAdminController(service), publicController)
				.setControllerAdvice(new ControllerExceptionHandler()).build();
	}

	@AfterEach
	void closeSession() {
		if (session != null) session.close();
	}

	@Test
	void migrationAndLegacyCreationDefaultToEmptyDescriptions() throws Exception {
		assertThat(service.getCategoryById(1L).getDescription()).isEmpty();
		mvc.perform(post("/admin/category").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"算法笔记\"}"))
				.andExpect(status().isOk());
		assertThat(service.getCategoryById(2L).getDescription()).isEmpty();
	}

	@Test
	void savesAndReadsTheDescriptionThroughBothAdminAndPublicLists() throws Exception {
		mvc.perform(post("/admin/category").contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"算法笔记\",\"description\":\"  从思路到实现，记录解题过程。  \"}"))
				.andExpect(status().isOk());
		mvc.perform(get("/categories")).andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].description").value("从思路到实现，记录解题过程。"));
		mvc.perform(get("/admin/categories")).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.list[0].description").value("从思路到实现，记录解题过程。"));
		assertThat(service.getCategoryById(2L).getDescription()).isEqualTo("从思路到实现，记录解题过程。");
		verify(redis).deleteCacheByKey(RedisKeyConstants.CATEGORY_NAME_LIST);
	}

	@Test
	void updatesDescriptionsPreservesOmittedValuesAndClearsExplicitBlanks() throws Exception {
		String description = "学".repeat(200);
		mvc.perform(put("/admin/category").contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\":1,\"name\":\"训练经验\",\"description\":\"" + description + "\"}"))
				.andExpect(status().isOk());
		verify(redis).deleteCacheByKey(RedisKeyConstants.CATEGORY_NAME_LIST);
		mvc.perform(put("/admin/category").contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\":1,\"name\":\"训练记录\"}"))
				.andExpect(status().isOk());
		assertThat(service.getCategoryById(1L).getDescription()).isEqualTo(description);
		mvc.perform(put("/admin/category").contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\":1,\"name\":\"训练记录\",\"description\":null}"))
				.andExpect(status().isOk());
		assertThat(service.getCategoryById(1L).getDescription()).isEqualTo(description);
		mvc.perform(put("/admin/category").contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\":1,\"name\":\"训练记录\",\"description\":\"   \"}"))
				.andExpect(status().isOk());
		mvc.perform(get("/categories")).andExpect(jsonPath("$.data[0].description").value(""));
	}

	@Test
	void rejectsOverlongDescriptionsWithoutWritingAnyChanges() throws Exception {
		String body = "{\"id\":1,\"name\":\"算法笔记\",\"description\":\"" + "学".repeat(201) + "\"}";
		mvc.perform(post("/admin/category").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"));
		mvc.perform(put("/admin/category").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"));
		assertThat(service.getCategoryList()).hasSize(1);
		assertThat(service.getCategoryById(1L).getName()).isEqualTo("训练经验");
		assertThat(service.getCategoryById(1L).getDescription()).isEmpty();
	}
}
