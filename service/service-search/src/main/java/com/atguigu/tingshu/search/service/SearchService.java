package com.atguigu.tingshu.search.service;

import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.atguigu.tingshu.model.album.BaseCategory3;
import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import com.atguigu.tingshu.query.search.AlbumIndexQuery;
import com.atguigu.tingshu.vo.search.AlbumSearchResponseVo;

import java.util.List;
import java.util.Map;

public interface SearchService {

    /**
     * 手动上架指定专辑到索引库
     * @param albumId
     * @return
     */
    void upperAlbum(Long albumId);

    /**
     * 手动从索引库下架指定专辑
     * @param albumId
     * @return
     */
    void lowerAlbum(Long albumId);

    /**
     * 站内搜索
     * @param albumIndexQuery
     * @return
     */
    AlbumSearchResponseVo search(AlbumIndexQuery albumIndexQuery);

    /**
     * 基于查询条件封装ES检索DSL语句
     * @param albumIndexQuery 查询条件
     * @return
     */
    SearchRequest buildDSL(AlbumIndexQuery albumIndexQuery);

    /**
     * 解析ES响应结果
     * @param searchResponse
     * @param albumIndexQuery
     * @return
     */
    AlbumSearchResponseVo parseResult(SearchResponse<AlbumInfoIndex> searchResponse, AlbumIndexQuery albumIndexQuery);

    /**
     * 查询置顶三级分类包含热门专辑列表
     * @param category1Id
     * @return
     */
    List<Map<String, Object>> channel(Long category1Id);

    /**
     * 构建 提示词文档对象 存入提示词索引库
     *
     * @param id
     * @param albumTitle
     */
    void saveSuggestInfo(Long id, String albumTitle);
}
