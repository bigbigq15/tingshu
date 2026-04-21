package com.atguigu.tingshu.album.service;

import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface AlbumInfoService extends IService<AlbumInfo> {

    void removeAlbumInfo(Long id);

    void saveAlbumInfo(AlbumInfoVo albumInfoVo, Long userId);

    List<AlbumInfo> findUserAllAlbumList(Long userId);

    void saveAlbumInfoStat(Long albumId, String statType, int statNum);

    IPage<AlbumListVo> findUserAlbumPageByUserId(IPage<AlbumListVo> pageInfo, AlbumInfoQuery albumInfoQuery);

    AlbumInfo getAlbumInfo(Long id);

    void updateAlbumInfo(Long id, AlbumInfoVo albumInfoVo);

}
