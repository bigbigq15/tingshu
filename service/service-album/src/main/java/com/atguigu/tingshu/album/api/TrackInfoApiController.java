package com.atguigu.tingshu.album.api;

import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.album.service.VodService;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Tag(name = "声音管理")
@RestController
@RequestMapping("api/album")
@SuppressWarnings({"all"})
public class TrackInfoApiController {

    @Autowired
    private TrackInfoService trackInfoService;

    @Autowired
    private VodService vodService;


    /**
     * 将音视频文件上传到腾讯云点播平台
     *
     * @param file
     * @return
     */
    @Operation(summary = "将音视频文件上传到腾讯云点播平台")
    @PostMapping("/trackInfo/uploadTrack")
    public Result<Map<String, String>> uploadTrack(@RequestParam("file") MultipartFile file){
        Map<String, String> map = vodService.uploadTrack(file);
        return Result.ok(map);
    }


    /**
     * TODO 该接口必须登录才能访问
     * 保存声音
     *
     * @param trackInfoVo 声音信息vo
     * @return
     */
    @Operation(summary = "保存声音")
    @PostMapping("/trackInfo/saveTrackInfo")
    public Result saveTrackInfo(@Validated @RequestBody TrackInfoVo trackInfoVo) {
        //1.获取当前用户ID
        Long userId = AuthContextHolder.getUserId();
        //2.调用业务逻辑
        trackInfoService.saveTrackInfo(trackInfoVo, userId);
        return Result.ok();
    }

    /**
     * TODO 当前接口必须才能访问
     * 条件分页查询当前用户声音列表
     *
     * @param page           页码
     * @param limit          页大小
     * @param trackInfoQuery 查询条件
     * @return MP分页对象
     */
    @Operation(summary = "条件分页查询当前用户声音列表")
    @PostMapping("/trackInfo/findUserTrackPage/{page}/{limit}")
    public Result<IPage<TrackListVo>> findUserTrackPage(
            @PathVariable Long page,
            @PathVariable Long limit,
            @RequestBody TrackInfoQuery trackInfoQuery
    ){
        Long userId = AuthContextHolder.getUserId();
        trackInfoQuery.setUserId(userId);
        IPage<TrackListVo> pageInfo = new Page<>(page, limit);
        pageInfo = trackInfoService.findUserTrackPage(pageInfo, trackInfoQuery);
        return Result.ok(pageInfo);
    }
}

