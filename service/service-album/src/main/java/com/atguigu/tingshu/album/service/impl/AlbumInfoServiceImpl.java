package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.atguigu.tingshu.album.mapper.AlbumAttributeValueMapper;
import com.atguigu.tingshu.album.mapper.AlbumInfoMapper;
import com.atguigu.tingshu.album.mapper.AlbumStatMapper;
import com.atguigu.tingshu.album.mapper.TrackInfoMapper;
import com.atguigu.tingshu.album.service.AlbumAttributeValueService;
import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.album.service.AlbumStatService;
import com.atguigu.tingshu.album.service.AuditService;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.atguigu.tingshu.common.rabbit.service.RabbitService;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.AlbumStat;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumAttributeValueVo;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import javax.sound.midi.Track;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.atguigu.tingshu.common.constant.SystemConstant.*;
import static com.atguigu.tingshu.common.constant.SystemConstant.ALBUM_STATUS_ARTIFICIAL;
import static com.atguigu.tingshu.common.result.ResultCodeEnum.ALBUM_NODE_ERROR;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class AlbumInfoServiceImpl extends ServiceImpl<AlbumInfoMapper, AlbumInfo> implements AlbumInfoService {

    @Autowired
    private AlbumInfoMapper albumInfoMapper;

    @Autowired
    private TrackInfoMapper trackInfoMapper;

    @Autowired
    private AlbumAttributeValueMapper albumAttributeValueMapper;

    @Autowired
    private AlbumAttributeValueService albumAttributeValueService;

    @Autowired
    private AlbumStatMapper albumStatMapper;

    @Autowired
    private AlbumStatService albumStatService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private RabbitService rabbitService;


    /**
     * 保存专辑信息
     *
     * @param albumInfoVo 专辑VO
     * @param userId      用户ID
     */
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
        if (CollUtil.isNotEmpty(albumAttributeValueVoList)) {
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

        // 4. 对新增专辑中文本：标题跟简介需要进行内容校验
        String text = albumInfo.getAlbumTitle() + albumInfo.getAlbumIntro();
        String suggest = auditService.auditText(text);
        if ("block".equals(suggest)) {
            albumInfo.setStatus(ALBUM_STATUS_NO_PASS);
        } else if ("review".equals(suggest)) {
            albumInfo.setStatus(ALBUM_STATUS_ARTIFICIAL);
        } else if ("pass".equals(suggest)) {
            albumInfo.setStatus(ALBUM_STATUS_PASS);
            //采用RabbitMQ可靠性消息 异步方式
            rabbitService.sendMessage(MqConst.EXCHANGE_ALBUM, MqConst.ROUTING_ALBUM_UPPER, albumInfoId);
        }
        albumInfoMapper.updateById(albumInfo);
    }

    /**
     * 保存专辑统计信息
     *
     * @param albumId  专辑ID
     * @param statType 统计类型
     * @param statNum  统计数值 0401-播放量 0402-订阅量 0403-购买量 0403-评论数'
     */
    @Override
    public void saveAlbumInfoStat(Long albumId, String statType, int statNum) {
        AlbumStat albumStat = new AlbumStat();
        albumStat.setAlbumId(albumId);
        albumStat.setStatNum(statNum);
        albumStat.setStatType(statType);
        albumStatMapper.insert(albumStat);
    }

    /**
     * 分页条件查询指定用户专辑列表
     *
     * @param pageInfo       分页对象
     * @param albumInfoQuery 查询条件
     * @return 分页对象
     */
    @Override
    public IPage<AlbumListVo> findUserAlbumPageByUserId(IPage<AlbumListVo> pageInfo, AlbumInfoQuery albumInfoQuery) {
        return albumInfoMapper.findUserAlbumPageByUserId(pageInfo, albumInfoQuery);
    }

    /**
     * 删除指定专辑
     *
     * @param id 专辑ID
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeAlbumInfo(Long id) {
        Long count = trackInfoMapper.selectCount(new LambdaQueryWrapper<TrackInfo>().eq(TrackInfo::getAlbumId, id));
        if (count > 0) {
            throw new GuiguException(ALBUM_NODE_ERROR);
        }
        albumInfoMapper.deleteById(id);
        albumStatMapper.delete(new LambdaQueryWrapper<AlbumStat>().eq(AlbumStat::getAlbumId, id));
        albumAttributeValueMapper.delete(new LambdaQueryWrapper<AlbumAttributeValue>().eq(AlbumAttributeValue::getAlbumId, id));
        rabbitService.sendMessage(MqConst.EXCHANGE_ALBUM, MqConst.ROUTING_ALBUM_LOWER, id);
    }

    /**
     * 查询专辑信息（包含标签列表）
     *
     * @param id
     * @return
     */
    @Override
    public AlbumInfo getAlbumInfo(Long id) {
        AlbumInfo albumInfo = this.getById(id);
        List<AlbumAttributeValue> albumAttributeValueList = albumAttributeValueService.list(new LambdaQueryWrapper<AlbumAttributeValue>().eq(AlbumAttributeValue::getAlbumId, id));
        if (CollUtil.isNotEmpty(albumAttributeValueList)) {
            albumInfo.setAlbumAttributeValueVoList(albumAttributeValueList);
        }
        return albumInfo;
    }

    /**
     * 修改专辑
     *
     * @param id          专辑ID
     * @param albumInfoVo 修改专辑VO信息
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAlbumInfo(Long id, AlbumInfoVo albumInfoVo) {
        AlbumInfo albumInfo = BeanUtil.copyProperties(albumInfoVo, AlbumInfo.class);
        albumInfo.setId(id);
        albumInfo.setStatus(ALBUM_STATUS_NO_PASS);
        this.updateById(albumInfo);

        albumAttributeValueService.remove(new LambdaQueryWrapper<AlbumAttributeValue>().eq(AlbumAttributeValue::getAlbumId, id));

        List<AlbumAttributeValueVo> albumAttributeValueVoList = albumInfoVo.getAlbumAttributeValueVoList();
        if (CollUtil.isNotEmpty(albumAttributeValueVoList)) {
            List<AlbumAttributeValue> collect = albumAttributeValueVoList.stream().map(new Function<AlbumAttributeValueVo, AlbumAttributeValue>() {
                @Override
                public AlbumAttributeValue apply(AlbumAttributeValueVo albumAttributeValueVo) {
                    AlbumAttributeValue albumAttributeValue = BeanUtil.copyProperties(albumAttributeValueVo, AlbumAttributeValue.class);
                    albumAttributeValue.setAlbumId(id);
                    return albumAttributeValue;
                }
            }).collect(Collectors.toList());
            albumAttributeValueService.saveBatch(collect);
        }

        // 验证新更改的文本
        String text = albumInfo.getAlbumTitle() + albumInfo.getAlbumIntro();
        String suggest = auditService.auditText(text);
        if ("block".equals(suggest)) {
            albumInfo.setStatus(ALBUM_STATUS_NO_PASS);
        } else if ("review".equals(suggest)) {
            albumInfo.setStatus(ALBUM_STATUS_ARTIFICIAL);
        } else if ("pass".equals(suggest)) {
            albumInfo.setStatus(ALBUM_STATUS_PASS);
            rabbitService.sendMessage(MqConst.EXCHANGE_ALBUM, MqConst.ROUTING_ALBUM_UPPER, id);
        }
        albumInfoMapper.update(albumInfo, new LambdaQueryWrapper<AlbumInfo>().eq(AlbumInfo::getId, id));

    }

    /**
     * 查询当前用户发布专辑列表
     * @param userId
     * @return
     */
    @Override
    public List<AlbumInfo> findUserAllAlbumList(Long userId) {
        LambdaQueryWrapper<AlbumInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AlbumInfo::getUserId, userId);
        queryWrapper.orderByDesc(AlbumInfo::getId);
        queryWrapper.last("LIMIT 200");
        queryWrapper.select(AlbumInfo::getId, AlbumInfo::getAlbumTitle);
        return albumInfoMapper.selectList(queryWrapper);
    }

    @Override
    public AlbumStatVo getAlbumStatVo(Long albumId) {
        AlbumStatVo albumStatVo = albumInfoMapper.getAlbumStatVo(albumId);
        return albumStatVo;
    }
}
