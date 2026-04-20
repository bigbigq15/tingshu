package com.atguigu.tingshu.album.api;

import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@Tag(name = "专辑管理")
@RestController
@RequestMapping("api/album")
@SuppressWarnings({"all"})
public class AlbumInfoApiController {

    @Autowired
    private AlbumInfoService albumInfoService;

    /**
     * TODO 该接口必须登录才能访问
     * 参数校验框架：@Validated 对请求体中VO中使用校验注解属性进行校验，底层基于AOP（前置通知）调用controller之前就会校验
     * 	校验成功才会调用controller方法，校验失败抛出异常，抛出的异常MethodArgumentNotValidException会进入全局异常处理类，返回给前端
     * @param albumInfoVo
     * @return
     */
    @Operation(summary = "保存专辑（内容创作者或运营管理人员）")
    @PostMapping("/albumInfo/saveAlbumInfo")
    public Result saveAlbumInfo(@Validated @RequestBody AlbumInfoVo albumInfoVo) {
        //1.从ThreadLocal获取当前登录用户ID
        Long userId = AuthContextHolder.getUserId();
        //2.调用service方法保存
        albumInfoService.saveAlbumInfo(albumInfoVo, userId);
        //3.返回结果
        return Result.ok();
    }


    public Result<List<AlbumInfo>> getUserAllAlbumList(){
        //1.从ThreadLocal中获取当前登录用户ID
        Long userId = AuthContextHolder.getUserId();
        //2.调用业务逻辑获取专辑列表
        List<AlbumInfo> list = albumInfoService.getUserAllAlbumList(userId);
        return  Result.ok(list);
    }


}

