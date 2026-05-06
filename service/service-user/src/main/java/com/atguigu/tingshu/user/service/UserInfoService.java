package com.atguigu.tingshu.user.service;

import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface UserInfoService extends IService<UserInfo> {
    /**
     * 微信小程序一键登录
     *
     * @param code 小程序集成微信获取访问微信账户基本信息临时凭据，用于获取微信账号唯一标识
     * @return {"token":"用户登录成功令牌"}
     */
    Map<String, String> wxLogin(String code);

    /**
     * 获取当前登录用户信息
     * @param userId
     * @return
     */
    UserInfoVo getUserInfo(Long userId);

    /**
     * 用户信息修改
     * @param userId 用户ID
     * @param userInfoVo 用户信息VO
     */
    void updateUser(Long userId, UserInfoVo userInfoVo);

    /**
     * 提交需要检查购买状态声音ID列表，响应每个声音购买状态
     *
     * @param userId
     * @param albumId
     * @param needCheckPayStatusTrackIdList
     * @return
     */
    Map<Long, Integer> userIsPaidTrack(Long userId, Long albumId, List<Long> needCheckPayStatusTrackIdList);

    /**
     * 根据专辑id+用户ID获取用户已购买声音id列表
     * @param albumId
     * @return
     */
    Boolean isPaidAlbum(Long userId, Long albumId);

    /**
     * 根据专辑id+用户ID获取用户已购买声音id列表
     * @param albumId
     * @return
     */
    List<Long> findUserPaidTrackIdList(Long userId, Long albumId);
}
