package com.atguigu.tingshu.user.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.atguigu.tingshu.common.rabbit.service.RabbitService;
import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.model.user.UserPaidAlbum;
import com.atguigu.tingshu.model.user.UserPaidTrack;
import com.atguigu.tingshu.user.mapper.UserInfoMapper;
import com.atguigu.tingshu.user.mapper.UserPaidAlbumMapper;
import com.atguigu.tingshu.user.mapper.UserPaidTrackMapper;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private WxMaService wxMaService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private UserPaidTrackMapper userPaidTrackMapper;

    @Autowired
    private UserPaidAlbumMapper userPaidAlbumMapper;


    /**
     * 微信小程序一键登录
     *
     * @param code 小程序集成微信获取访问微信账户基本信息临时凭据，用于获取微信账号唯一标识
     * @return {"token":"用户登录成功令牌"}
     */
    @Override
    public Map<String, String> wxLogin(String code) {
        try {
            //1.调用微信接口获取微信账户唯一标识
            WxMaJscode2SessionResult sessionInfo = wxMaService.getUserService().getSessionInfo(code);
            String wxOpenId = sessionInfo.getOpenid();

            //2.根据微信账户唯一标识 判断是否存在
            UserInfo userInfo = userInfoMapper.selectOne(
                    new LambdaQueryWrapper<UserInfo>()
                            .eq(UserInfo::getWxOpenId, wxOpenId)
            );

            //3.如果首次登录，将微信唯一标识关联到自定义用户记录且新增，同时为用户新增账户记录
            if (userInfo == null) {
                //3.1 新增用户记录关联微信账户唯一标识
                userInfo = new UserInfo();
                userInfo.setWxOpenId(wxOpenId);
                userInfo.setAvatarUrl("http://192.168.200.6:9000/tingshu/2026-04-20/3dcb2f53a4d14b3db5638f689f573162.png");
                userInfo.setNickname("听友" + IdUtil.nanoId());
                userInfoMapper.insert(userInfo);
                //3.2采用RabbitMQ异步新增对应账户记录
                //3.2.1 准备需要发送业务数据 Map封装或者Vo对象（必须实现序列化接口）
                HashMap<String, Object> msgData = new HashMap<>();
                msgData.put("userId", userInfo.getId());
                msgData.put("title", "新用户专项体验金");
                msgData.put("amount", new BigDecimal("100"));
                msgData.put("orderNo", "zs" + IdUtil.getSnowflakeNextId());
                //3.2.2 调用生产者发送消息工具方法发送消息
                rabbitService.sendMessage(MqConst.EXCHANGE_USER, MqConst.ROUTING_USER_REGISTER, msgData);
            }

            //4.基于用户基本信息生成令牌，将用户存入Redis
            //4.1 为用户生成令牌 UUID方式
            String token = IdUtil.randomUUID();
            //4.2 构建用户登录key 形式：前缀:token
            String loginKey = RedisConstant.USER_LOGIN_KEY_PREFIX + token;
            //4.3 存入Redis key:登录key value:用户基本信息：UserInfoVo 存7天有效期
            UserInfoVo userInfoVo = BeanUtil.copyProperties(userInfo, UserInfoVo.class);
            redisTemplate.opsForValue().set(loginKey, userInfoVo, RedisConstant.USER_LOGIN_KEY_TIMEOUT, TimeUnit.SECONDS);
            //5.返回令牌
            return Map.of("token", token);
        } catch (WxErrorException e) {
            log.error("微信登录异常", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取当前登录用户信息
     *
     * @param userId
     * @return
     */
    @Override
    public UserInfoVo getUserInfo(Long userId) {
        UserInfo userInfo = this.getById(userId);
        if (userInfo != null) {
            return BeanUtil.copyProperties(userInfo, UserInfoVo.class);
        }
        return null;
    }

    @Override
    public void updateUser(Long userId, UserInfoVo userInfoVo) {
        //1.只允许修改昵称、头像
        UserInfo userInfo = new UserInfo();
        userInfo.setId(userId);
        userInfo.setNickname(userInfoVo.getNickname());
        userInfo.setAvatarUrl(userInfoVo.getAvatarUrl());
        //2.修改
        userInfoMapper.updateById(userInfo);
    }

    /**
     * 提交需要检查购买状态声音ID列表，响应每个声音购买状态
     *
     * @param userId                        用户ID
     * @param albumId                       专辑ID
     * @param needCheckPayStatusTrackIdList 待检查购买状态声音ID列表
     * @return
     */
    @Override
    public Map<Long, Integer> userIsPaidTrack(Long userId, Long albumId, List<Long> needCheckPayStatusTrackIdList) {
        Map<Long, Integer> map = new HashMap<>();
        //1.根据用户ID+专辑ID查询专辑购买记录
        Long count = userPaidAlbumMapper.selectCount(
                new LambdaQueryWrapper<UserPaidAlbum>()
                        .eq(UserPaidAlbum::getUserId, userId)
                        .eq(UserPaidAlbum::getAlbumId, albumId)
        );
        //2. 如果已购买专辑，将所有待检查购买状态声音 购买状态设置为 1 响应
        if (count > 0){
            for (Long trackId : needCheckPayStatusTrackIdList) {
                map.put(trackId, 1);
            }
            return map;
        }
        //3. 根据用户ID+专辑ID查询已购声音记录
        List<UserPaidTrack> userPaidTrackList = userPaidTrackMapper.selectList(
                new LambdaQueryWrapper<UserPaidTrack>()
                        .eq(UserPaidTrack::getUserId, userId)
                        .eq(UserPaidTrack::getAlbumId, albumId)
                        .select(UserPaidTrack::getTrackId)
        );
        //4. 如果不存再已购声音，将所有待检查购买状态声音 购买状态设置为 0 响应
        if(CollUtil.isEmpty(userPaidTrackList)){
            for (Long trackId : needCheckPayStatusTrackIdList) {
                map.put(trackId, 0);
            }
            return map;
        }
        //5.如果存在已购声音，将提交检查声音ID列表中，已购声购买状态设置为：1。未购买设置为0
        List<Long> userPaidTrackIdList = userPaidTrackList.stream().map(UserPaidTrack::getTrackId).collect(Collectors.toList());
        for (Long trackId : needCheckPayStatusTrackIdList) {
            if (userPaidTrackIdList.contains(trackId)) {
                map.put(trackId, 1);
            } else {
                map.put(trackId, 0);
            }
        }
        return map;
    }

    /**
     * 判断指定用户是否购买指定专辑
     *
     * @param albumId
     * @return 购买状态：true:已购买专辑、 false:未购买专辑
     */
    @Override
    public Boolean isPaidAlbum(Long userId, Long albumId) {
        Long count = userPaidAlbumMapper.selectCount(
                new LambdaQueryWrapper<UserPaidAlbum>()
                        .eq(UserPaidAlbum::getAlbumId, albumId)
                        .eq(UserPaidAlbum::getUserId, userId)
        );
        return count > 0;
    }

    /**
     * 根据专辑id+用户ID获取用户已购买声音id列表
     *
     * @param albumId
     * @return
     */
    @Override
    public List<Long> findUserPaidTrackIdList(Long userId, Long albumId) {
        List<UserPaidTrack> userPaidTrackList = userPaidTrackMapper.selectList(
                new LambdaQueryWrapper<UserPaidTrack>()
                        .eq(UserPaidTrack::getAlbumId, albumId)
                        .eq(UserPaidTrack::getUserId, userId)
                        .select(UserPaidTrack::getTrackId)
        );
        if (CollUtil.isNotEmpty(userPaidTrackList)) {
            List<Long> paidTrackIdList = userPaidTrackList.stream()
                    .map(UserPaidTrack::getTrackId)
                    .collect(Collectors.toList());
            return paidTrackIdList;
        }
        return List.of();
    }
}
