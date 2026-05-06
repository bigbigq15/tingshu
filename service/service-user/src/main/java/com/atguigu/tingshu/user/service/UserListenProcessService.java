package com.atguigu.tingshu.user.service;

import com.atguigu.tingshu.vo.user.UserListenProcessVo;

import java.math.BigDecimal;
import java.util.Map;

public interface UserListenProcessService {

    /**
     * 获取用户某个声音播放进度
     * @param userId
     * @param trackId
     * @return
     */
    BigDecimal getTrackBreakSecond(Long userId, Long trackId);

    /**
     * 更新声音播放进度
     * @param userId
     * @param userListenProcessVo
     */
    void updateListenProcess(Long userId, UserListenProcessVo userListenProcessVo);

    /**
     * 获取当前用户最近播放声音
     * @return {albumId:1,trackId:12}
     */
    Map<String, Long> getLatelyTrack(Long userId);
}
