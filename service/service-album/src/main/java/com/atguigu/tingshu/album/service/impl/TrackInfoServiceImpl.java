package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.atguigu.tingshu.album.config.VodConstantProperties;
import com.atguigu.tingshu.album.mapper.AlbumInfoMapper;
import com.atguigu.tingshu.album.mapper.TrackInfoMapper;
import com.atguigu.tingshu.album.mapper.TrackStatMapper;
import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.album.service.VodService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import static com.atguigu.tingshu.common.constant.SystemConstant.*;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.album.TrackStat;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.atguigu.tingshu.vo.album.TrackMediaInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qcloud.vod.VodUploadClient;
import com.qcloud.vod.model.VodUploadRequest;
import com.qcloud.vod.model.VodUploadResponse;
import com.tencentcloudapi.common.Credential;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class TrackInfoServiceImpl extends ServiceImpl<TrackInfoMapper, TrackInfo> implements TrackInfoService {

	@Autowired
	private TrackInfoMapper trackInfoMapper;

	@Autowired
	private AlbumInfoMapper albumInfoMapper;

	@Autowired
	private VodConstantProperties vodConstantProperties;

	@Autowired
	private VodService vodService;

	@Autowired
	private TrackStatMapper trackStatMapper;



	@Override
	public void saveTrackInfo(TrackInfoVo trackInfoVo, Long userId) {
		//1.根据专辑ID查询专辑信息 用于更新声音数量
		AlbumInfo albumInfo = albumInfoMapper.selectById(trackInfoVo.getAlbumId());
		//2.保存声音记录、更新专辑内包含声音数量
		//2.1 将声音VO转为PO对象
		TrackInfo trackInfo = BeanUtil.copyProperties(trackInfoVo, TrackInfo.class);
		//2.2 封装声音属性信息
		//2.2.1 基础：用户ID、状态、来源、封面图片
		trackInfo.setUserId(userId);
		trackInfo.setStatus(SystemConstant.TRACK_STATUS_NO_PASS);
		if (StringUtils.isBlank(trackInfoVo.getCoverUrl())) {
			trackInfo.setCoverUrl(albumInfo.getCoverUrl());
		}
		trackInfo.setSource(SystemConstant.TRACK_SOURCE_USER);
		//2.2.2 设置序号=专辑包含声音数量+1
		trackInfo.setOrderNum(albumInfo.getIncludeTrackCount() + 1);
		//2.2.2 从点播平台获取，声音时长、大小、类型
		TrackMediaInfoVo mediaInfoVo = vodService.getMediaInfo(trackInfo.getMediaFileId());
		if (mediaInfoVo != null) {
			trackInfo.setMediaDuration(BigDecimal.valueOf(mediaInfoVo.getDuration()));
			trackInfo.setMediaSize(mediaInfoVo.getSize());
			trackInfo.setMediaType(mediaInfoVo.getType());
		}
		//2.3 保存声音得到声音ID
		trackInfoMapper.insert(trackInfo);
		Long trackInfoId = trackInfo.getId();

		//2.4 更新专辑包含声音数量
		albumInfo.setIncludeTrackCount(albumInfo.getIncludeTrackCount() + 1);
		albumInfoMapper.updateById(albumInfo);

		//3.新增统计信息
		this.saveTrackStat(trackInfoId, SystemConstant.TRACK_STAT_PLAY, 0);
		this.saveTrackStat(trackInfoId, SystemConstant.TRACK_STAT_COLLECT, 0);
		this.saveTrackStat(trackInfoId, SystemConstant.TRACK_STAT_PRAISE, 0);
		this.saveTrackStat(trackInfoId, SystemConstant.TRACK_STAT_COMMENT, 0);

		//todo
		//审核


	}

	@Override
	public void saveTrackStat(Long trackId, String statType, int statNum) {
		TrackStat trackStat = new TrackStat();
		trackStat.setTrackId(trackId);
		trackStat.setStatType(statType);
		trackStat.setStatNum(statNum);
		trackStatMapper.insert(trackStat);
	}

	@Override
	public IPage<TrackListVo> findUserTrackPage(IPage<TrackListVo> pageInfo, TrackInfoQuery trackInfoQuery) {
		return baseMapper.findUserTrackPage(pageInfo,trackInfoQuery);
	}
}
