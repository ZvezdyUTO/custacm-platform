package top.naccl.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import top.naccl.entity.ImageAsset;

import java.util.Date;
import java.util.List;

/**
 * @author huangbingrui.awa
 */
@Mapper
public interface ImageAssetMapper {
	@Insert("""
			insert into image_asset
			(public_id, owner_user_id, purpose, original_path, thumbnail_path, original_url, thumbnail_url,
			 mime_type, width, height, original_bytes, thumbnail_bytes, status, create_time, update_time)
			values
			(#{publicId}, #{ownerUserId}, #{purpose}, #{originalPath}, #{thumbnailPath}, #{originalUrl}, #{thumbnailUrl},
			 #{mimeType}, #{width}, #{height}, #{originalBytes}, #{thumbnailBytes}, #{status}, now(), now())
			""")
	@Options(useGeneratedKeys = true, keyProperty = "id")
	int insert(ImageAsset asset);

	@Select("select * from image_asset where id=#{id}")
	ImageAsset findById(Long id);

	@Select("""
			select ia.*, bir.blog_id, bir.role as reference_role
			from image_asset ia left join blog_image_reference bir on bir.image_asset_id=ia.id
			where ia.id=#{id}
			""")
	ImageAsset findByIdWithReference(Long id);

	@Select("""
			<script>
			select ia.*, bir.blog_id, bir.role as reference_role
			from image_asset ia left join blog_image_reference bir on bir.image_asset_id=ia.id
			where ia.public_id in
			<foreach collection="publicIds" item="publicId" open="(" separator="," close=")">
				#{publicId}
			</foreach>
			</script>
			""")
	List<ImageAsset> findByPublicIdsWithReference(@Param("publicIds") List<String> publicIds);

	@Select("""
			select ia.*
			from image_asset ia left join blog_image_reference bir on bir.image_asset_id=ia.id
			where ia.owner_user_id=#{ownerUserId} and bir.image_asset_id is null
			order by ia.id
			""")
	List<ImageAsset> findUnreferencedByOwnerUserId(Long ownerUserId);

	@Select("""
			select ia.*, bir.blog_id, bir.role as reference_role
			from image_asset ia join blog_image_reference bir on bir.image_asset_id=ia.id
			where bir.blog_id=#{blogId}
			""")
	List<ImageAsset> findByBlogId(Long blogId);

	@Select("""
			<script>
			select ia.*, bir.blog_id, bir.role as reference_role
			from image_asset ia join blog_image_reference bir on bir.image_asset_id=ia.id
			where bir.blog_id in
			<foreach collection="blogIds" item="blogId" open="(" separator="," close=")">
				#{blogId}
			</foreach>
			order by bir.blog_id, ia.id
			</script>
			""")
	List<ImageAsset> findByBlogIds(@Param("blogIds") List<Long> blogIds);

	@Select("""
			<script>
			select * from image_asset where id in
			<foreach collection="ids" item="id" open="(" separator="," close=")">
				#{id}
			</foreach>
			order by id
			</script>
			""")
	List<ImageAsset> findByIds(@Param("ids") List<Long> ids);

	@Select("select blog_id from blog_image_reference where image_asset_id=#{assetId}")
	Long findReferencedBlogId(Long assetId);

	@Delete("delete from blog_image_reference where blog_id=#{blogId}")
	int deleteReferencesByBlogId(Long blogId);

	@Insert("insert into blog_image_reference (blog_id, image_asset_id, role) values (#{blogId}, #{assetId}, #{role})")
	int insertReference(@Param("blogId") Long blogId, @Param("assetId") Long assetId, @Param("role") String role);

	@Update("update image_asset set status=#{status}, update_time=now() where id=#{id}")
	int updateStatus(@Param("id") Long id, @Param("status") String status);

	@Delete("delete from image_asset where id=#{id}")
	int deleteById(Long id);

	@Select("""
			select * from image_asset
			where status='DELETING' or (status='TEMP' and create_time &lt; #{cutoff})
			order by id
			""")
	List<ImageAsset> findCleanupCandidates(Date cutoff);

	@Select("select public_id from image_asset")
	List<String> findAllPublicIds();
}
