package top.naccl.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserMapperConcurrencyTest {
    @Test
    void accountMutationWaitsForThePreviousWriterAndReadsItsCommittedState() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:users-" + UUID.randomUUID()
                + ";MODE=MySQL;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE user (id BIGINT PRIMARY KEY, username VARCHAR_IGNORECASE(128) UNIQUE, "
                + "password VARCHAR(255), update_time TIMESTAMP)");
        jdbc.update("INSERT INTO user (id, username, password) VALUES (1, 'root', 'original-hash')");
        Configuration configuration = new Configuration(new Environment(
                "test", new JdbcTransactionFactory(), dataSource));
        configuration.setMapUnderscoreToCamelCase(true);
        try (InputStream mapperXml = getClass().getResourceAsStream("/mapper/UserMapper.xml")) {
            new XMLMapperBuilder(mapperXml, configuration, "mapper/UserMapper.xml",
                    configuration.getSqlFragments()).parse();
        }
        SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(configuration);
        var executor = Executors.newSingleThreadExecutor();
        try (SqlSession first = factory.openSession(false)) {
            UserMapper mapper = first.getMapper(UserMapper.class);
            assertEquals("root", mapper.findByUsernameForUpdate("ROOT").getUsername());
            CountDownLatch started = new CountDownLatch(1);
            var nextWriter = executor.submit(() -> {
                try (SqlSession second = factory.openSession(false)) {
                    started.countDown();
                    return second.getMapper(UserMapper.class).findByUsernameForUpdate("root").getPassword();
                }
            });
            assertTrue(started.await(5, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class, () -> nextWriter.get(100, TimeUnit.MILLISECONDS));
            mapper.updatePasswordByUsername("root", "new-hash");
            first.commit();
            assertEquals("new-hash", nextWriter.get(5, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }
}
