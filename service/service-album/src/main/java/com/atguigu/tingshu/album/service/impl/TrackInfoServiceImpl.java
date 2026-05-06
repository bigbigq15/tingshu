package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import com.atguigu.tingshu.album.config.VodConstantProperties;
import com.atguigu.tingshu.album.mapper.AlbumInfoMapper;
import com.atguigu.tingshu.album.mapper.AlbumStatMapper;
import com.atguigu.tingshu.album.mapper.TrackInfoMapper;
import com.atguigu.tingshu.album.mapper.TrackStatMapper;
import com.atguigu.tingshu.album.service.AuditService;
import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.album.service.VodService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.AlbumStat;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.album.TrackStat;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.album.*;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static com.atguigu.tingshu.common.constant.SystemConstant.*;


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

    @Autowired
    private AuditService auditService;

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private AlbumStatMapper albumStatMapper;


    /**
     * 保存声音
     *
     * @param trackInfoVo 声音信息vo
     * @param userId      用户ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveTrackInfo(TrackInfoVo trackInfoVo, Long userId) {
        //1.根据专辑ID查询专辑信息 用于更新声音数量
        AlbumInfo albumInfo = albumInfoMapper.selectById(trackInfoVo.getAlbumId());
        //2.保存声音记录、更新专辑内包含声音数量
        //2.1 将声音VO转为PO对象
        TrackInfo trackInfo = BeanUtil.copyProperties(trackInfoVo, TrackInfo.class);
        //2.2 封装声音属性信息
        //2.2.1 基础：用户ID、状态、来源、封面图片
        trackInfo.setUserId(userId);
        trackInfo.setStatus(TRACK_STATUS_NO_PASS);
        if (StringUtils.isBlank(trackInfoVo.getCoverUrl())) {
            trackInfo.setCoverUrl(albumInfo.getCoverUrl());
        }
        trackInfo.setSource(TRACK_SOURCE_USER);
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
        this.saveTrackStat(trackInfoId, TRACK_STAT_PLAY, 0);
        this.saveTrackStat(trackInfoId, TRACK_STAT_COLLECT, 0);
        this.saveTrackStat(trackInfoId, TRACK_STAT_PRAISE, 0);
        this.saveTrackStat(trackInfoId, TRACK_STAT_COMMENT, 0);

        //审核
        String text = trackInfo.getTrackTitle() + trackInfo.getTrackIntro();
        String suggest = auditService.auditText(text);
        if ("block".equals(suggest)) {
            trackInfo.setStatus(TRACK_STATUS_NO_PASS);
        } else if ("review".equals(suggest)) {
            trackInfo.setStatus(TRACK_STATUS_ARTIFICIAL);
        } else if ("pass".equals(suggest)) {
            trackInfo.setStatus(TRACK_STATUS_PASS);
            //5.对上传的声音文件发起审核任务ID，关联审核任务ID
            String taskId = auditService.startReviewTask(trackInfo.getMediaFileId());
            trackInfo.setReviewTaskId(taskId);
            trackInfo.setStatus(TRACK_STATUS_REVIEWING);
        }
        trackInfoMapper.updateById(trackInfo);


    }

    @Override
    public void saveTrackStat(Long trackId, String statType, int statNum) {
        TrackStat trackStat = new TrackStat();
        trackStat.setTrackId(trackId);
        trackStat.setStatType(statType);
        trackStat.setStatNum(statNum);
        trackStatMapper.insert(trackStat);
    }

    /**
     * 条件分页查询当前用户声音列表
     *
     * @param pageInfo       分页对象
     * @param trackInfoQuery 查询条件
     * @return 分页对象
     */
    @Override
    public IPage<TrackListVo> findUserTrackPage(IPage<TrackListVo> pageInfo, TrackInfoQuery trackInfoQuery) {
        return baseMapper.findUserTrackPage(pageInfo, trackInfoQuery);
    }

    /**
     * 修改声音信息
     *
     * @param id          声音Id
     * @param trackInfoVo 声音信息VO
     * @return
     */
    @Override
    public void updateTrackInfo(Long id, TrackInfoVo trackInfoVo) {
        Boolean isNeedReview = false;
        //1.判断音频文件是否更新，如果更新，再次获取新音频文件详情.
        //1.1 根据声音ID查询声音信息，获取原来的音频ID
        TrackInfo trackInfo = this.getById(id);
        String oldMediaFileId = trackInfo.getMediaFileId();
        //1.2 封装更新后：标题、简介、封面图片、音频URL、唯一标识
        BeanUtil.copyProperties(trackInfoVo, trackInfo);
        //1.3 对比声音vo中音频ID判断是否变更
        if (!oldMediaFileId.equals(trackInfoVo.getMediaFileId())) {
            //1.4 如果变更，再次调用平台接口获取音频详情，更新音频相关信息：时长、大小、类型、播放地址、
            TrackMediaInfoVo mediaInfo = vodService.getMediaInfo(trackInfoVo.getMediaFileId());
            trackInfo.setMediaDuration(BigDecimal.valueOf(mediaInfo.getDuration()));
            trackInfo.setMediaType(mediaInfo.getType());
            trackInfo.setMediaSize(mediaInfo.getSize());
            //4.5 将旧音频文件从点播平台删除
            vodService.deleteMedia(oldMediaFileId);
            isNeedReview = true;
        }

        //2.更新声音信息，修改后文本同样需要进行内容审核
        trackInfo.setStatus(TRACK_STATUS_NO_PASS);

        //4.对声音中文本进行内容审核
        String text = trackInfo.getTrackTitle() + trackInfo.getTrackIntro();
        String suggest = auditService.auditText(text);
        if ("block".equals(suggest)) {
            trackInfo.setStatus(TRACK_STATUS_NO_PASS);
        } else if ("review".equals(suggest)) {
            trackInfo.setStatus(TRACK_STATUS_ARTIFICIAL);
        } else if ("pass".equals(suggest)) {
            trackInfo.setStatus(TRACK_STATUS_PASS);
            if (isNeedReview) {
                String taskId = auditService.startReviewTask(trackInfo.getMediaFileId());
                trackInfo.setReviewTaskId(taskId);
                trackInfo.setStatus(TRACK_STATUS_REVIEWING);
            }
        }
        trackInfoMapper.updateById(trackInfo);
    }

    /**
     * 删除声音
     *
     * @param id 声音ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeTrackInfo(Long id) {
        //1.根据声音ID查询声音信息，得到专辑ID、声音序号
        TrackInfo trackInfo = this.getById(id);
        Long albumId = trackInfo.getAlbumId();
        Integer orderNum = trackInfo.getOrderNum();

        //2.根据专辑ID查询专辑信息，用于 更新专辑声音数量
        AlbumInfo albumInfo = albumInfoMapper.selectById(albumId);
        albumInfo.setIncludeTrackCount(albumInfo.getIncludeTrackCount() - 1);
        albumInfoMapper.updateById(albumInfo);

        //3.先更新专辑下大于被删声音序号 确保声音需要连续
        trackInfoMapper.update(
                null,
                new LambdaUpdateWrapper<TrackInfo>()
                        .eq(TrackInfo::getAlbumId, albumId)
                        .gt(TrackInfo::getOrderNum, orderNum)
                        .setSql("order_num = order_num - 1 ")
        );

        //4.删除声音记录
        trackInfoMapper.deleteById(id);

        // 5.删除统计信息
        trackStatMapper.delete(
                new LambdaQueryWrapper<TrackStat>().eq(TrackStat::getTrackId, id)
        );

        //6.从点播平台删除音频文件
        vodService.deleteMedia(trackInfo.getMediaFileId());

    }

    /**
     * 需求：用户未登录，可以给用户展示声音列表；用户已登录，可以给用户展示声音列表，并动态渲染付费标识
     * 分页查询专辑下声音列表（动态渲染付费标识）
     *
     * @param pageInfo MP分页对象
     * @param albumId  专辑ID
     * @param userId   用户ID
     * @return
     */
    @Override
    public IPage<AlbumTrackListVo> findAlbumTrackPage(IPage<AlbumTrackListVo> pageInfo, Long albumId, Long userId) {
        //1.分页获取声音列表（包含统计数值） isShowPaidMark默认为false
        pageInfo = trackInfoMapper.findAlbumTrackPage(pageInfo, albumId);

        //动态渲染付费标识
        //2.根据专辑ID查询专辑信息，得到专辑付费类型以及免费试听的集数
        AlbumInfo albumInfo = albumInfoMapper.selectById(albumId);
        Assert.notNull(albumInfo, "专辑{}不存在", albumId);
        //2.1 付费类型：0101-免费、0102-vip免费、0103-付费
        String payType = albumInfo.getPayType();
        //2.2 免费实体集数
        Integer tracksForFree = albumInfo.getTracksForFree();
        //3.如果用户未登录 情况一：且专辑付费类型是：VIP免费或付费，将除试听以外其他声音付费标识设置true
        if (userId == null) {
            if (ALBUM_PAY_TYPE_VIPFREE.equals(payType) || ALBUM_PAY_TYPE_REQUIRE.equals(payType)) {
                pageInfo.getRecords()
                        .stream()
                        .filter(t -> t.getOrderNum() > tracksForFree)
                        .forEach(t -> t.setIsShowPaidMark(true));
            }
        } else {
            //4. 如果用户已登录 满足以下两种情况需要进一步获取当前页声音购买状态
            //4.1 远程调用用户服务获取用户身份，判断是否为会员（有效期内会员）
            UserInfoVo userInfoVo = userFeignClient.getUserInfoVo(userId).getData();
            Assert.notNull(userInfoVo, "用户{}不存在", userId);
            Boolean isVIP = false;
            if (userInfoVo.getIsVip().intValue() == 1 && userInfoVo.getVipExpireTime().after(new Date())) {
                isVIP = true;
            }
            //4.2 情况二：如果专辑付费类型：VIP免费 且 当前用户是普通用户，满足进一步查询当前页声音购买状态
            Boolean isNeedCheckPayStatus = false;
            if (!isVIP && ALBUM_PAY_TYPE_VIPFREE.equals(payType)) {
                isNeedCheckPayStatus = true;
            }
            //4.3 情况三：如果专辑 付费类型：付费，无论什么用户，满足进一步查询当前页声音购买状态
            if (ALBUM_PAY_TYPE_REQUIRE.equals(payType)) {
                isNeedCheckPayStatus = true;
            }
            //4.4 如果满足情况二或情况三，远程调用用户服务，得到当前页除试听部分每个声音购买状态
            if (isNeedCheckPayStatus) {
                //4.4.1 获取当前页中除试听以外其他声音ID列表
                List<Long> needCheckPayStatusTrackIdList = pageInfo
                        .getRecords()
                        .stream()
                        .filter(t -> t.getOrderNum() > tracksForFree)
                        .map(AlbumTrackListVo::getTrackId)
                        .collect(Collectors.toList());
                //4.4.2 远程调用用户服务，获取当前页中声音购买状态Map<Long-专辑ID,Integer-购买状态
                Map<Long, Integer> payStatusMap =
                        userFeignClient.userIsPaidTrack(userId, albumId, needCheckPayStatusTrackIdList).getData();

                //4.5 根据响应声音购买状态，动态修改付费标识。购买状态为0的声音付费标识isShowPaidMark全部设置：true
                pageInfo
                        .getRecords()
                        .stream()
                        .filter(t -> t.getOrderNum() > tracksForFree)
                        .forEach(t -> t.setIsShowPaidMark(payStatusMap.get(t.getTrackId()) == 0));
            }
        }
        return pageInfo;
    }

    /**
     * 更新声音以及所属专辑统计设置
     *
     * @param mqVo
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStat(TrackStatMqVo mqVo) {
        //1.更新声音统计表
        trackStatMapper.update(
                null,
                new LambdaUpdateWrapper<TrackStat>()
                        .eq(TrackStat::getTrackId, mqVo.getTrackId())
                        .eq(TrackStat::getStatType, mqVo.getStatType())
                        .setSql("stat_num = stat_num +" + mqVo.getCount())
        );
        //2.如果是播放或评论统计类型 同时 更新所属专辑统计数值
        if (TRACK_STAT_PLAY.equals(mqVo.getStatType())) {
            albumStatMapper.update(
                    null,
                    new LambdaUpdateWrapper<AlbumStat>()
                            .eq(AlbumStat::getAlbumId, mqVo.getAlbumId())
                            .eq(AlbumStat::getStatType, ALBUM_STAT_PLAY)
                            .setSql("stat_num = stat_num +" + mqVo.getCount())
            );
        }
        if (TRACK_STAT_COMMENT.equals(mqVo.getStatType())) {
            albumStatMapper.update(
                    null,
                    new LambdaUpdateWrapper<AlbumStat>()
                            .eq(AlbumStat::getAlbumId, mqVo.getAlbumId())
                            .eq(AlbumStat::getStatType, ALBUM_STAT_COMMENT)
                            .setSql("stat_num = stat_num +" + mqVo.getCount())
            );
        }
    }

    /**
     * 以选择购买声音作为起始，基于未购买声音数量，返回分集购买列表
     *
     * @param trackId 选择购买声音ID
     * @return [{name:"本集",price:0.1,trackCount:1},{name:"后10集",price:1,trackCount:10}..]
     */
    @Override
    public List<Map<String, Object>> findFenJiPaidList(Long userId, Long trackId) {
        //1.根据声音ID查询选择 "欲购"的声音信息 得到所属专辑ID，序号
        TrackInfo trackInfo = trackInfoMapper.selectById(trackId);
        Long albumId = trackInfo.getAlbumId();
        Integer orderNum = trackInfo.getOrderNum();
        //2.查询"待购买"声音列表（可能包含已购买声音）
        List<TrackInfo> waitBuyTrackList = trackInfoMapper.selectList(
                new LambdaQueryWrapper<TrackInfo>()
                        .eq(TrackInfo::getAlbumId, albumId)
                        .ge(TrackInfo::getOrderNum, orderNum)
                        .select(TrackInfo::getId)
        );

        //3.远程调用"用户服务"获取该专辑下已购买声音ID列表
        List<Long> paidTrackIdList = userFeignClient.findUserPaidTrackIdList(albumId).getData();

        //4.将已购买声音去掉，得到最终代购买声音数量
        if (CollUtil.isNotEmpty(paidTrackIdList)) {
            waitBuyTrackList = waitBuyTrackList.stream()
                    .filter(t -> !paidTrackIdList.contains(t.getId()))
                    .collect(Collectors.toList());
        }

        //5.基于未购买声音数量，构建分集购买对象集合
        //5.0 查询所属专辑得到声音单价
        AlbumInfo albumInfo = albumInfoMapper.selectById(albumId);
        BigDecimal price = albumInfo.getPrice();
        //未购买声音数量
        int size = waitBuyTrackList.size();
        ArrayList<Map<String, Object>> mapList = new ArrayList<>();
        //5.1 构建"本集"分集购买对象
        mapList.add(Map.of("name", "本集", "trackCount", 1, "price", price));
        //5.2 构建"后N集"分集购买对象，最多显示5个
        for (int i = 10; i <= 50; i += 10) {
            if (i < size) {
                mapList.add(Map.of("name", "后" + i + "集", "trackCount", i, "price", price.multiply(BigDecimal.valueOf(i))));
            } else {
                mapList.add(Map.of("name", "后" + size + "集(全集)", "trackCount", size, "price", price.multiply(BigDecimal.valueOf(size))));
                break;
            }
        }
        return mapList;
    }

    /**
     * 以用户选择声音作为起始，查询当前用户未购买声音列表，展示订单确认页
     *
     * @param userId
     * @param trackId
     * @param trackCount
     * @return
     */
    @Override
    public List<TrackInfo> findPaidTrackInfoList(Long userId, Long trackId, Integer trackCount) {
        //1.根据声音ID查询欲购声音信息 得到专辑ID跟序号
        TrackInfo trackInfo = trackInfoMapper.selectById(trackId);
        Long albumId = trackInfo.getAlbumId();
        Integer orderNum = trackInfo.getOrderNum();

        //2.构建查询对象
        LambdaQueryWrapper<TrackInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TrackInfo::getAlbumId, albumId);
        queryWrapper.ge(TrackInfo::getOrderNum, orderNum);
        //2.1.远程调用"用户服务"获取已购声音ID列表
        List<Long> paidTrackIdList = userFeignClient.findUserPaidTrackIdList(albumId).getData();
        if(CollUtil.isNotEmpty(paidTrackIdList)){
            queryWrapper.notIn(TrackInfo::getId, paidTrackIdList);
        }
        //2.2 增加排序，限制返回数量,限制查询字段
        queryWrapper.orderByAsc(TrackInfo::getOrderNum);
        queryWrapper.last("limit "+trackCount);
        queryWrapper.select(TrackInfo::getId,TrackInfo::getAlbumId, TrackInfo::getCoverUrl, TrackInfo::getTrackIntro, TrackInfo::getTrackTitle);

        //3.查询未购买声音列表
        List<TrackInfo> waitBuyTrackInfoList = trackInfoMapper.selectList(queryWrapper);
        return waitBuyTrackInfoList;
    }
}
