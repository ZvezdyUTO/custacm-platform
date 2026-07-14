package top.naccl.controller;

import org.junit.jupiter.api.Test;
import top.naccl.model.vo.HomepageFeaturedImage;
import top.naccl.model.vo.Result;
import top.naccl.service.HomepageFeaturedImageService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author huangbingrui.awa
 */
class HomepageFeaturedImageControllerTest {
    @Test
    void returnsEveryConfiguredImageInServiceOrder() {
        HomepageFeaturedImageService service = mock(HomepageFeaturedImageService.class);
        List<HomepageFeaturedImage> images = List.of(
                new HomepageFeaturedImage(2L, "/two.jpg", "/two-thumb.jpg", 0),
                new HomepageFeaturedImage(1L, "/one.jpg", "/one-thumb.jpg", 1));
        when(service.list()).thenReturn(images);

        Result result = new HomepageFeaturedImageController(service).list();

        assertEquals(images, result.getData());
        verify(service).list();
    }
}
