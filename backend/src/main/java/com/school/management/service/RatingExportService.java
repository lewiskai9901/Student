package com.school.management.service;

import com.school.management.dto.export.RatingExportRequest;
import com.school.management.dto.rating.RatingFrequencyVO;
import com.school.management.dto.rating.RatingResultVO;
import com.school.management.dto.statistics.ClassRankingVO;

import java.util.List;

/**
 * 评级数据导出服务接口
 */
public interface RatingExportService {

    /**
     * 导出评级结果
     *
     * @param request 导出请求
     * @return Excel字节数组
     */
    byte[] exportRatingResults(RatingExportRequest request);

    /**
     * 导出评级频次统计
     *
     * @param request 导出请求
     * @return Excel字节数组
     */
    byte[] exportRatingFrequency(RatingExportRequest request);

    /**
     * 导出班级排名统计
     *
     * @param request 导出请求
     * @return Excel字节数组
     */
    byte[] exportClassRanking(RatingExportRequest request);

    /**
     * 导出指定等级的班级列表（如导出所有优秀班级）
     *
     * @param request 导出请求
     * @return Excel字节数组
     */
    byte[] exportClassesByLevel(RatingExportRequest request);

    /**
     * 获取导出文件名
     *
     * @param exportType 导出类型
     * @param format 文件格式
     * @return 文件名
     */
    String getExportFileName(String exportType, String format);
}
