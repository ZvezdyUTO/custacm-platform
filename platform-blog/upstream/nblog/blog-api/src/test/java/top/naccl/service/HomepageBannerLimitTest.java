package top.naccl.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.naccl.config.properties.UploadProperties;
import top.naccl.exception.BadRequestException;
import top.naccl.repository.HomepageBannerRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * @author huangbingrui.awa
 */
@ExtendWith(MockitoExtension.class)
class HomepageBannerLimitTest {
    @Mock
    private HomepageBannerRepository repository;

    @Test
    void rejectsUploadWhenTwoHomepageImagesAlreadyExist() {
        when(repository.count()).thenReturn(2);
        HomepageBannerService service = new HomepageBannerService(repository, new UploadProperties());

        BadRequestException error = assertThrows(BadRequestException.class, () -> service.upload(null));

        assertEquals("首页最多保留两张图片", error.getMessage());
    }
}
