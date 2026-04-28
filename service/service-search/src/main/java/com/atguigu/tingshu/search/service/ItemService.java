package com.atguigu.tingshu.search.service;

import java.util.Map;

public interface ItemService {


    /**
     * 根据专辑ID汇总详情页所需参数
     *
     * @param albumId
     * @return
     */
    //ItemVo getItem(Long albumId);

    Map<String, Object> getItem(Long albumId);
}

