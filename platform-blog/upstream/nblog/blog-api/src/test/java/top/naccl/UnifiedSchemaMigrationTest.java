package top.naccl;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnifiedSchemaMigrationTest {
    @Test
    void baselineDoesNotSeedAWellKnownAdministratorPassword() throws IOException {
        String migration = resource("/db/migration/V001__create_nblog_schema.sql");

        assertTrue(migration.contains("CREATE TABLE `user`"));
        assertFalse(migration.contains("INSERT INTO `user` VALUES"));
    }

    @Test
    void integrationMigrationUsesUsernameAndPreservesContentOnUserDeletion() throws IOException {
        String migration = resource("/db/migration/V024__integrate_blog_users_and_training_handles.sql");

        assertTrue(migration.contains("RENAME COLUMN student_identity TO username"));
        assertTrue(migration.contains("ON UPDATE CASCADE ON DELETE CASCADE"));
        assertTrue(migration.contains("ON DELETE SET NULL"));
        assertTrue(migration.contains("ROLE_admin"));
        assertTrue(migration.contains("ROLE_player"));
    }

	@Test
	void addsUserSignatureAndOwnedProfileLinks() throws IOException {
		String migration = resource("/db/migration/V027__add_user_profile_and_links.sql");

		assertTrue(migration.contains("ADD COLUMN `signature` varchar(160) NOT NULL DEFAULT ''"));
		assertTrue(migration.contains("CREATE TABLE `user_profile_link`"));
		assertTrue(migration.contains("FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)"));
		assertTrue(migration.contains("ON DELETE CASCADE"));
	}

	@Test
	void removesArticleViewCountingAndItsScheduledSyncJob() throws IOException {
		String migration = resource("/db/migration/V033__remove_blog_views.sql");

		assertTrue(migration.contains("ALTER TABLE blog DROP COLUMN views"));
		assertTrue(migration.contains("bean_name = 'redisSyncScheduleTask'"));
		assertTrue(migration.contains("method_name = 'syncBlogViewsToDatabase'"));
	}

    private static String resource(String path) throws IOException {
        try (InputStream input = UnifiedSchemaMigrationTest.class.getResourceAsStream(path)) {
            if (input == null) {
                return "";
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
