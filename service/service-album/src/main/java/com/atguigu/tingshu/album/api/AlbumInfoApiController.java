package com.atguigu.tingshu.album.api;

import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Tag(name = "专辑管理")
@RestController
@RequestMapping("api/album")
@SuppressWarnings({"all"})
public class AlbumInfoApiController {

    @Autowired
    private AlbumInfoService albumInfoService;

    /**
     * 参数校验框架：@Validated 对请求体中VO中使用校验注解属性进行校验，底层基于AOP（前置通知）调用controller之前就会校验
     * 校验成功才会调用controller方法，校验失败抛出异常，抛出的异常MethodArgumentNotValidException会进入全局异常处理类，返回给前端
     *
     * @param albumInfoVo
     * @return
     */
    @GuiGuLogin
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

    /**
     * 分页查询当前用户专辑列表
     *
     * @param page  页码
     * @param limit 页大小
     * @return 分页对象 列表中专辑信息（包含统计信息）
     */
    @GuiGuLogin
    @Operation(summary = "分页查询当前用户专辑列表")
    @PostMapping("/albumInfo/findUserAlbumPage/{page}/{limit}")
    public Result<IPage<AlbumListVo>> findUserAlbumPageByUserId(@PathVariable Long page,
                                                                @PathVariable Long limit,
                                                                @RequestBody AlbumInfoQuery albumInfoQuery) {
        Long userId = AuthContextHolder.getUserId();

        IPage<AlbumListVo> pageInfo = new Page<>(page, limit);
        albumInfoQuery.setUserId(userId);
        pageInfo = albumInfoService.findUserAlbumPageByUserId(pageInfo, albumInfoQuery);
        return Result.ok(pageInfo);
    }

    /**
     * 删除指定专辑
     *
     * @param id 专辑ID
     * @return
     */
    @Operation(summary = "删除专辑")
    @DeleteMapping("/albumInfo/removeAlbumInfo/{id}")
    public Result removeAlbumInfo(@PathVariable Long id) {
        albumInfoService.removeAlbumInfo(id);
        return Result.ok();
    }

    /**
     * 查询专辑信息（包含标签列表）
     *
     * @param id
     * @return
     */
    @Operation(summary = "查询专辑信息（包含标签列表）")
    @GetMapping("/albumInfo/getAlbumInfo/{id}")
    public Result<AlbumInfo> getAlbumInfo(@PathVariable Long id) {
        AlbumInfo albumInfo = albumInfoService.getAlbumInfo(id);
        return Result.ok(albumInfo);
    }


    /**
     * 修改专辑
     *
     * @param id          专辑ID
     * @param albumInfoVo 修改专辑VO信息
     * @return
     */
    @Operation(summary = "修改专辑")
    @PutMapping("/albumInfo/updateAlbumInfo/{id}")
    public Result updateAlbumInfo(@PathVariable Long id, @Validated @RequestBody AlbumInfoVo albumInfoVo) {
        albumInfoService.updateAlbumInfo(id, albumInfoVo);
        return Result.ok();
    }


    /**
     * 查询当前用户发布专辑列表
     *
     * @return
     */
    @GuiGuLogin
    @Operation(summary = "查询当前用户发布专辑列表")
    @GetMapping("/albumInfo/findUserAllAlbumList")
    public Result<List<AlbumInfo>> findUserAllAlbumList() {
        //1.从ThreadLocal中获取当前登录用户ID
        Long userId = AuthContextHolder.getUserId();
        //2.调用业务逻辑获取专辑列表
        List<AlbumInfo> list = albumInfoService.findUserAllAlbumList(userId);
        return Result.ok(list);
    }

    /**
     * 根据专辑ID查询专辑统计信息
     * @param albumId
     * @return
     */
    @Operation(summary = "根据专辑ID查询专辑统计信息")
    @GetMapping("/albumInfo/getAlbumStatVo/{albumId}")
    public Result<AlbumStatVo> getAlbumStatVo(@PathVariable Long albumId){
        AlbumStatVo albumStatVo = albumInfoService.getAlbumStatVo(albumId);
        return Result.ok(albumStatVo);
    }

}

