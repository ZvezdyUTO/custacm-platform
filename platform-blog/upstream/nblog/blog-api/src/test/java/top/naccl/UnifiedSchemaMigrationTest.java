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

	@Test
	void expandsLegacyOjHandleJsonIntoRelationalBindingsWithoutDroppingLegacyData() throws IOException {
		String migration = resource("/db/migration/V034__normalize_oj_handle_accounts.sql");

		assertTrue(migration.contains("CREATE TABLE training_member"));
		assertTrue(migration.contains("CREATE TABLE oj_handle_binding"));
		assertTrue(migration.contains("CREATE TEMPORARY TABLE tmp_oj_handle_binding_v034"));
		assertTrue(migration.contains("DROP TABLE IF EXISTS oj_handle_binding"));
		assertTrue(migration.contains("DROP TABLE IF EXISTS training_member"));
		assertTrue(migration.contains("PRIMARY KEY (username, oj_name)"));
		assertTrue(migration.contains(
				"UNIQUE KEY uk_oj_handle_binding_oj_name_handle (oj_name, handle)"));
		assertTrue(migration.contains(
				"handle varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL"));
		assertTrue(migration.contains("FOREIGN KEY (username) REFERENCES `user` (username)"));
		assertTrue(migration.contains("FOREIGN KEY (username) REFERENCES training_member (username)"));
		assertTrue(migration.contains("JSON_EXTRACT(handles_json, '$.CODEFORCES')"));
		assertTrue(migration.contains("JSON_EXTRACT(handles_json, '$.ATCODER')"));
		assertTrue(migration.contains("$.CODEFORCES.lastCollectedAt"));
		assertTrue(migration.contains("$.ATCODER.lastCollectedAt"));
		assertFalse(migration.toLowerCase().contains("drop table oj_handle_account"));
		assertFalse(migration.toLowerCase().contains("drop column handles_json"));
	}

	@Test
	void removesTablesOwnedOnlyByRetiredPagesAndRuntimeSubsystems() throws IOException {
		String migration = resource("/db/migration/V035__drop_retired_nblog_features.sql");

		for (String table : new String[]{
				"about", "friend", "moment", "schedule_job", "schedule_job_log",
				"exception_log", "operation_log", "login_log", "visit_log",
				"visit_record", "city_visitor", "visitor"
		}) {
			assertTrue(migration.contains("DROP TABLE IF EXISTS " + table));
		}
		assertFalse(migration.contains("DROP TABLE IF EXISTS site_setting"));
		assertFalse(migration.contains("DROP TABLE IF EXISTS comment"));
	}

	@Test
	void addsASevenDayArticleRecycleBinMarkerAndLookupIndex() throws IOException {
		String migration = resource("/db/migration/V036__add_blog_recycle_bin.sql");

		assertTrue(migration.contains("ADD COLUMN deleted_at DATETIME(0) NULL"));
		assertTrue(migration.contains("ADD INDEX idx_blog_recycle_bin (deleted_at)"));
	}

	@Test
	void createsCompetitionRecordsWithHistoricalIdentityAndCascadeContracts() throws IOException {
		String migration = resource("/db/migration/V037__create_competition_records.sql");
		String normalized = migration.replaceAll("\\s+", " ");

		assertTrue(migration.contains("CREATE TABLE competition ("));
		assertTrue(migration.contains("UNIQUE KEY uk_competition_active_full_name (active_full_name)"));
		assertTrue(migration.contains("CHECK (competition_year BETWEEN 1900 AND 9999)"));
		assertTrue(migration.contains("CREATE TABLE competition_type_tag ("));
		assertTrue(migration.contains("PRIMARY KEY (competition_id, type)"));

		assertTrue(migration.contains("CREATE TABLE competition_participant ("));
		assertTrue(migration.contains("display_name_snapshot varchar(255)"));
		assertTrue(normalized.contains(
				"FOREIGN KEY (username) REFERENCES `user` (username) ON UPDATE CASCADE ON DELETE SET NULL"));

		assertTrue(migration.contains("CHECK (award_level BETWEEN 1 AND 4)"));
		assertTrue(migration.contains(
				"CHECK (rank_position > 0 AND rank_total > 0 AND rank_position <= rank_total)"));
		assertTrue(migration.contains("profile_visible boolean NOT NULL DEFAULT false"));
		assertTrue(migration.contains("CHECK (profile_visible IN (false, true))"));

		assertTrue(normalized.contains(
				"FOREIGN KEY (award_id, competition_id) REFERENCES competition_award (id, competition_id) ON DELETE CASCADE"));
		assertTrue(normalized.contains(
				"FOREIGN KEY (participant_id, competition_id) REFERENCES competition_participant (id, competition_id) ON DELETE CASCADE"));
		assertTrue(normalized.contains(
				"FOREIGN KEY (participant_id) REFERENCES competition_participant (id) ON DELETE CASCADE"));
		assertTrue(normalized.contains("FOREIGN KEY (blog_id) REFERENCES blog (id) ON DELETE CASCADE"));
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
