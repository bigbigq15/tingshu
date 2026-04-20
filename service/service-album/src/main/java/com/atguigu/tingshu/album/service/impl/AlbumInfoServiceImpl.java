package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.atguigu.tingshu.album.mapper.AlbumAttributeValueMapper;
import com.atguigu.tingshu.album.mapper.AlbumInfoMapper;
import com.atguigu.tingshu.album.mapper.AlbumStatMapper;
import com.atguigu.tingshu.album.service.AlbumAttributeValueService;
import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.AlbumStat;
import com.atguigu.tingshu.vo.album.AlbumAttributeValueVo;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.atguigu.tingshu.common.constant.SystemConstant.*;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class AlbumInfoServiceImpl extends ServiceImpl<AlbumInfoMapper, AlbumInfo> implements AlbumInfoService {

    @Autowired
    private AlbumInfoMapper albumInfoMapper;

    @Autowired
    private AlbumAttributeValueMapper albumAttributeValueMapper;

    @Autowired
    private AlbumAttributeValueService albumAttributeValueService;

    @Autowired
    private AlbumStatMapper albumStatMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAlbumInfo(AlbumInfoVo albumInfoVo, Long userId) {
        AlbumInfo albumInfo = BeanUtil.copyProperties(albumInfoVo, AlbumInfo.class);
        albumInfo.setUserId(userId);
        albumInfo.setStatus(ALBUM_STATUS_NO_PASS);
        String payType = albumInfo.getPayType();
        if (ALBUM_PAY_TYPE_VIPFREE.equals(payType) || ALBUM_PAY_TYPE_REQUIRE.equals(payType)) {
            albumInfo.setTracksForFree(5);
        }
        albumInfoMapper.insert(albumInfo);
        Long albumInfoId = albumInfo.getId();
        List<AlbumAttributeValueVo> albumAttributeValueVoList = albumInfoVo.getAlbumAttributeValueVoList();
        if(CollUtil.isNotEmpty(albumAttributeValueVoList)){
            List<AlbumAttributeValue> collect = albumAttributeValueVoList.stream().map(new Function<AlbumAttributeValueVo, AlbumAttributeValue>() {
                @Override
                public AlbumAttributeValue apply(AlbumAttributeValueVo albumAttributeValueVo) {
                    AlbumAttributeValue albumAttributeValue = BeanUtil.copyProperties(albumAttributeValueVo, AlbumAttributeValue.class);
                    albumAttributeValue.setAlbumId(albumInfoId);
                    return albumAttributeValue;
                }
            }).collect(Collectors.toList());
            albumAttributeValueService.saveBatch(collect);
        }
        this.saveAlbumInfoStat(albumInfoId, ALBUM_STAT_PLAY, 0);
        this.saveAlbumInfoStat(albumInfoId, ALBUM_STAT_SUBSCRIBE, 0);
        this.saveAlbumInfoStat(albumInfoId, ALBUM_STAT_BUY, 0);
        this.saveAlbumInfoStat(albumInfoId, ALBUM_STAT_COMMENT, 0);
        //TODO,校验

    }

    @Override
    public void saveAlbumInfoStat(Long albumId, String statType, int statNum) {
        AlbumStat albumStat = new AlbumStat();
        albumStat.setAlbumId(albumId);
        albumStat.setStatNum(statNum);
        albumStat.setStatType(statType);
        albumStatMapper.insert(albumStat);
    }

    @Override
    public List<AlbumInfo> getUserAllAlbumList(Long userId) {
        LambdaQueryWrapper<AlbumInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AlbumInfo::getUserId,userId);
        queryWrapper.orderByDesc(AlbumInfo::getId);
        queryWrapper.last("LIMIT 200");
        queryWrapper.select(AlbumInfo::getId, AlbumInfo::getAlbumTitle);
        return albumInfoMapper.selectList(queryWrapper);
    }
}
