package top.naccl.controller.player;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import top.naccl.entity.ImageAsset;
import top.naccl.exception.ImageAssetException;
import top.naccl.model.vo.Result;
import top.naccl.service.ImageAssetService;

/**
 * @author huangbingrui.awa
 */
@RestController
@RequestMapping("/player/images")
public class PlayerImageController {
	private final ImageAssetService imageAssetService;

	public PlayerImageController(ImageAssetService imageAssetService) {
		this.imageAssetService = imageAssetService;
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public Result upload(Authentication authentication, @RequestPart("file") MultipartFile file,
			@RequestPart("purpose") String purposeValue) {
		ImageAsset.Purpose purpose;
		try {
			purpose = ImageAsset.Purpose.valueOf(purposeValue);
		} catch (IllegalArgumentException | NullPointerException exception) {
			throw new ImageAssetException(ImageAssetException.ErrorCode.IMAGE_FORMAT_UNSUPPORTED, "图片用途不正确");
		}
		return Result.ok("上传成功", imageAssetService.upload(authentication.getName(), file, purpose));
	}

	@DeleteMapping("/{id}")
	public Result delete(Authentication authentication, @PathVariable Long id) {
		imageAssetService.deleteUnbound(authentication.getName(), id);
		return Result.ok("删除成功");
	}
}
