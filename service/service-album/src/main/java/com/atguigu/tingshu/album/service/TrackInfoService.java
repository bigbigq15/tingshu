package com.atguigu.tingshu.album.service;

import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumTrackListVo;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.atguigu.tingshu.vo.album.TrackStatMqVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface TrackInfoService extends IService<TrackInfo> {
    /**
     * 保存声音
     * @param trackInfoVo  声音信息vo
     * @param userId 用户ID
     */
    void saveTrackInfo(TrackInfoVo trackInfoVo, Long userId);

    /**
     * 保存声音统计信息
     * @param trackId 声音ID
     * @param statType 统计类型
     * @param statNum 统计数值
     */
    void saveTrackStat(Long trackId, String statType, int statNum);

    /**
     * 条件分页查询当前用户声音列表
     * @param pageInfo 分页对象
     * @param trackInfoQuery 查询条件
     * @return 分页对象
     */
    IPage<TrackListVo> findUserTrackPage(IPage<TrackListVo> pageInfo, TrackInfoQuery trackInfoQuery);

    /**
     * 修改声音信息
     * @param id 声音Id
     * @param trackInfoVo 声音信息VO
     * @return
     */
    void updateTrackInfo(Long id, TrackInfoVo trackInfoVo);

    /**
     * 删除声音
     * @param id 声音ID
     */
    void removeTrackInfo(Long id);

    /**
     * 需求：用户未登录，可以给用户展示声音列表；用户已登录，可以给用户展示声音列表，并动态渲染付费标识
     * 分页查询专辑下声音列表（动态渲染付费标识）
     *
     * @param pageInfo MP分页对象
     * @param albumId 专辑ID
     * @param userId 用户ID
     * @return
     */
    IPage<AlbumTrackListVo> findAlbumTrackPage(IPage<AlbumTrackListVo> pageInfo, Long albumId, Long userId);

    /**
     * 更新声音以及所属专辑统计设置
     * @param mqVo
     */
    void updateStat(TrackStatMqVo mqVo);

    /**
     * 以选择购买声音作为起始，基于未购买声音数量，返回分集购买列表
     * @param trackId 选择购买声音ID
     * @return [{name:"本集",price:0.1,trackCount:1},{name:"后10集",price:1,trackCount:10}..]
     */
    List<Map<String, Object>> findFenJiPaidList(Long userId, Long trackId);

    /**
     * 以用户选择声音作为起始，查询当前用户未购买声音列表，展示订单确认页
     *
     * @param userId
     * @param trackId
     * @param trackCount
     * @return
     */
    List<TrackInfo> findPaidTrackInfoList(Long userId, Long trackId, Integer trackCount);
}
