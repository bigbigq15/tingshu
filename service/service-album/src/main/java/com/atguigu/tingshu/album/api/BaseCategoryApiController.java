package com.atguigu.tingshu.album.api;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.service.BaseCategoryService;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.BaseAttribute;
import com.atguigu.tingshu.model.album.BaseCategory3;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@Tag(name = "分类管理")
@RestController
@RequestMapping(value = "/api/album")
@SuppressWarnings({"all"})
public class BaseCategoryApiController {

    @Autowired
    private BaseCategoryService baseCategoryService;

    /**
     * 查询所有分类（1、2、3级分类）
     *
     * @return 业务数据：[{"categoryId":1,"categoryName":"分类",categoryChild:[..]},{其他1级分类}]
     */
    @Operation(summary = "查询所有分类（1、2、3级分类）")
    @GetMapping("/category/getBaseCategoryList")
    public Result<List<JSONObject>> getBaseCategoryList() {
        List<JSONObject> list = baseCategoryService.getBaseCategoryList();
        return Result.ok(list);
    }

    /**
     * 根据一级分类Id获取分类属性以及属性值（标签名，标签值）列表
     *
     * @param category1Id
     * @return
     */
    @Operation(summary = "根据一级分类Id获取分类属性以及属性值（标签名，标签值）列表")
    @GetMapping("/category/findAttribute/{category1Id}")
    public Result<List<BaseAttribute>> findAttribute(@PathVariable Long category1Id) {
        List<BaseAttribute> list = baseCategoryService.findAttribute(category1Id);
        return Result.ok(list);
    }

    /**
     * 根据3级分类ID查询分类视图对象
     * @param category3Id
     * @return
     */
    @Operation(summary = "根据3级分类ID查询分类视图对象")
    @GetMapping("/category/getCategoryView/{category3Id}")
    public Result<BaseCategoryView> getCategoryView(@PathVariable Long category3Id){
        BaseCategoryView baseCategoryView = baseCategoryService.getCategoryView(category3Id);
        return Result.ok(baseCategoryView);
    }

    /**
     * TODO 后续使用Redis缓存
     * 根据1级分类ID查询置顶前七个三级分类列表
     * @param category1Id 1级分类ID
     * @return 三级分类列表
     */
    @Operation(summary = "根据1级分类ID查询置顶前七个三级分类列表")
    @GetMapping("/category/findTopBaseCategory3/{category1Id}")
    public Result<List<BaseCategory3>> findTop7BaseCategory3(@PathVariable Long category1Id){
        List<BaseCategory3> list = baseCategoryService.findTop7BaseCategory3(category1Id);
        return Result.ok(list);
    }

    /**
     * TODO 后续使用Redis缓存
     * 查询1级分类下包含所有2,3级分类列表
     * @param category1Id
     * @return 1级分类对象 包含2,3级分类列表
     */
    @Operation(summary = "查询1级分类下包含所有2,3级分类列表")
    @GetMapping("/category/getBaseCategoryList/{category1Id}")
    public Result<JSONObject> getBaseCategoryListByCategory1Id(@PathVariable Long category1Id){
        JSONObject jsonObject = baseCategoryService.getBaseCategoryListByCategory1Id(category1Id);
        return Result.ok(jsonObject);
    }

}

