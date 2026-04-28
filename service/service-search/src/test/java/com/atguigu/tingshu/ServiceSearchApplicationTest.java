package com.atguigu.tingshu;

import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.search.service.SearchService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;
@Slf4j
@SpringBootTest
class ServiceSearchApplicationTest {

    @Autowired
    private AlbumFeignClient albumFeignClient;


    @Test
    public void testFeign() {
        Result<AlbumInfo> result = albumFeignClient.getAlbumInfo(1L);
        System.out.println(result);
    }


    @Autowired
    private SearchService searchService;

    /**
     * 不严谨批量导入
     * 从专辑ID1开始到最大专辑ID
     */
    @Test
    public void testBatchImport() {
        for (Long i = 1L; i <= 1644; i++) {
            try {
                searchService.upperAlbum(i);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void log(){
        log.info("1");
        log.warn("2");
        log.error("3");
        log.debug("4");
    }

}
