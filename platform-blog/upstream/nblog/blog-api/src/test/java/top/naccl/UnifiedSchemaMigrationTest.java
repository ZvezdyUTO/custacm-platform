package top.naccl;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnifiedSchemaMigrationTest {
	@Test
	void taxonomyNamesAreDatabaseUnique() throws IOException {
		String categoryMigration = resource("/db/migration/V043__enforce_category_name_uniqueness.sql")
				.replaceAll("\\s+", " ");
		String tagMigration = resource("/db/migration/V044__enforce_tag_name_uniqueness.sql")
				.replaceAll("\\s+", " ");

		assertTrue(categoryMigration.contains("ADD CONSTRAINT uk_category_name UNIQUE (category_name)"));
		assertFalse(categoryMigration.contains("ALTER TABLE tag"));
		assertTrue(tagMigration.contains("ADD CONSTRAINT uk_tag_name UNIQUE (tag_name)"));
		assertFalse(tagMigration.contains("ALTER TABLE category"));
	}

	@Test
	void taxonomyReferencesAndJoinPairsAreDatabaseEnforced() throws IOException {
		String pairMigration = normalizedResource(
				"/db/migration/V045__enforce_blog_tag_pair_uniqueness.sql");
		String tagIndexMigration = normalizedResource(
				"/db/migration/V046__index_blog_tag_by_tag.sql");
		String categoryReferenceMigration = normalizedResource(
				"/db/migration/V047__enforce_blog_category_reference.sql");
		String blogReferenceMigration = normalizedResource(
				"/db/migration/V048__enforce_blog_tag_blog_reference.sql");
		String tagReferenceMigration = normalizedResource(
				"/db/migration/V049__enforce_blog_tag_tag_reference.sql");

		assertTrue(pairMigration.contains("ALTER TABLE blog_tag ADD PRIMARY KEY (blog_id, tag_id)"));
		assertTrue(tagIndexMigration.contains(
				"ALTER TABLE blog_tag ADD INDEX idx_blog_tag_tag_id_blog_id (tag_id, blog_id)"));
		assertTrue(categoryReferenceMigration.contains(
				"FOREIGN KEY (category_id) REFERENCES category (id) ON UPDATE RESTRICT ON DELETE RESTRICT"));
		assertTrue(blogReferenceMigration.contains(
				"FOREIGN KEY (blog_id) REFERENCES blog (id) ON UPDATE CASCADE ON DELETE CASCADE"));
		assertTrue(tagReferenceMigration.contains(
				"FOREIGN KEY (tag_id) REFERENCES tag (id) ON UPDATE CASCADE ON DELETE RESTRICT"));
	}

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

	@Test
	void makesOrdinaryAwardRanksOptionalAndAddsOrderedProfileAchievements() throws IOException {
		String migration = resource("/db/migration/V041__normalize_competition_awards_and_profile_order.sql");
		String normalized = migration.replaceAll("\\s+", " ");

		assertTrue(normalized.contains("DROP CHECK chk_competition_award_rank"));
		assertTrue(normalized.contains("MODIFY COLUMN rank_position int unsigned NULL"));
		assertTrue(normalized.contains("MODIFY COLUMN rank_total int unsigned NULL"));
		assertTrue(normalized.contains("(rank_position IS NULL AND rank_total IS NULL) OR"));
		assertTrue(normalized.contains("ADD COLUMN profile_sort_order bigint unsigned NULL"));
		assertTrue(normalized.contains("SET profile_sort_order = award_id WHERE profile_visible = true"));
		assertTrue(normalized.contains("profile_visible = false AND profile_sort_order IS NULL"));
		assertTrue(normalized.contains("c.full_name = '第 50 届 ICPC 亚洲区域赛（西安）'"));
		assertTrue(normalized.contains("ctt.type = 'ASIA_REGIONAL'"));
		assertFalse(normalized.toUpperCase().contains(" LIKE "));
		assertFalse(normalized.toUpperCase().contains(" REGEXP "));
	}

	@Test
	void addsAnExplicitLoginRequirementToCompetitionAwards() throws IOException {
		String migration = normalizedResource(
				"/db/migration/V050__add_competition_award_login_requirement.sql");

		assertTrue(migration.contains(
				"ADD COLUMN requires_login boolean NOT NULL DEFAULT false AFTER award_name"));
		assertTrue(migration.contains(
				"CHECK (requires_login IN (false, true))"));
	}

	@Test
	void addsAnOptionalCompetitionDateWhilePreservingHistoricalYears() throws IOException {
		String migration = normalizedResource(
				"/db/migration/V051__add_competition_date.sql");

		assertTrue(migration.contains("DROP CHECK chk_competition_year"));
		assertTrue(migration.contains("MODIFY COLUMN competition_year smallint unsigned NULL"));
		assertTrue(migration.contains("ADD COLUMN competition_date date NULL AFTER competition_year"));
		assertTrue(migration.contains(
				"CHECK (competition_year IS NULL OR competition_year BETWEEN 1900 AND 9999)"));
		assertTrue(migration.contains(
				"competition_date IS NULL OR (competition_year IS NOT NULL AND YEAR(competition_date) = competition_year)"));
		assertFalse(migration.toUpperCase().contains("UPDATE COMPETITION"));
	}

	@Test
	void competitionCategoryQueriesMatchTheWholeStableTypeSetBeforePaging() throws IOException {
		String mapper = resource("/mapper/CompetitionMapper.xml");
		String normalized = mapper.replaceAll("\\s+", " ");

		assertTrue(mapper.contains("categoryTypes != null and !categoryTypes.isEmpty()"));
		assertTrue(mapper.contains("= #{categoryTypeCount}"));
		assertTrue(mapper.contains("collection=\"categoryTypes\""));
		assertTrue(mapper.contains("ctt.type = #{categoryType}"));
		assertTrue(mapper.indexOf("id=\"findActiveCompetitions\"")
				< mapper.indexOf("id=\"findRecycleBinCompetitions\""));
		assertTrue(normalized.contains(
				"order by coalesce(c.competition_date, makedate(c.competition_year, 1)) desc, "
						+ "c.competition_date is null, c.id desc"));
	}

	@Test
	void createsHomepageFeaturedGroupsWithThreeOrderedGloballyUniqueArticles() throws IOException {
		String migration = resource("/db/migration/V038__create_homepage_featured_groups.sql");
		String normalized = migration.replaceAll("\\s+", " ");

		assertTrue(migration.contains("CREATE TABLE homepage_featured_group ("));
		assertTrue(migration.contains("CREATE TABLE homepage_featured_group_article ("));
		assertTrue(migration.contains("UNIQUE KEY uk_homepage_featured_group_article_blog (blog_id)"));
		assertTrue(migration.contains(
				"UNIQUE KEY uk_homepage_featured_group_article_sort_order (group_id, sort_order)"));
		assertTrue(migration.contains("CHECK (sort_order BETWEEN 0 AND 2)"));
		assertTrue(normalized.contains(
				"FOREIGN KEY (group_id) REFERENCES homepage_featured_group (id) ON DELETE CASCADE"));
		assertTrue(normalized.contains("FOREIGN KEY (blog_id) REFERENCES blog (id) ON DELETE CASCADE"));
		assertTrue(normalized.contains(
				"ORDER BY is_top DESC, is_recommend DESC, update_time DESC, id DESC"));
		assertTrue(migration.contains("ROW_NUMBER() OVER"));
		assertTrue(migration.contains("WHERE is_published = true"));
		assertTrue(migration.contains("AND is_internal = false"));
		assertTrue(migration.contains("AND deleted_at IS NULL"));
	}

	@Test
	void createsAnOrderedTwelveSlotHomepageFeaturedImageCollection() throws IOException {
		String migration = resource("/db/migration/V039__create_homepage_featured_images.sql");

		assertTrue(migration.contains("CREATE TABLE `homepage_featured_image`"));
		assertTrue(migration.contains("`image_url` varchar(1024) NOT NULL"));
		assertTrue(migration.contains("UNIQUE KEY `uk_homepage_featured_image_sort_order` (`sort_order`)"));
	}

	@Test
	void retiresDynamicHomepageBannersAndAddsFeaturedImageThumbnails() throws IOException {
		String migration = resource("/db/migration/V040__retire_homepage_banners_and_add_featured_thumbnails.sql");

		assertTrue(migration.contains("DROP TABLE `homepage_banner_image`"));
		assertTrue(migration.contains("ADD COLUMN `thumbnail_url` varchar(1024) NULL"));
	}

	private static String resource(String path) throws IOException {
        try (InputStream input = UnifiedSchemaMigrationTest.class.getResourceAsStream(path)) {
            if (input == null) {
                return "";
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

	private static String normalizedResource(String path) throws IOException {
		return resource(path).replaceAll("\\s+", " ").trim();
	}
}
