package com.atguigu.tingshu.album.service.impl;


import com.atguigu.tingshu.album.mapper.AlbumStatMapper;
import com.atguigu.tingshu.album.service.AlbumStatService;
import com.atguigu.tingshu.model.album.AlbumStat;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class AlbumStatServiceImpl extends ServiceImpl<AlbumStatMapper, AlbumStat> implements AlbumStatService {
}
