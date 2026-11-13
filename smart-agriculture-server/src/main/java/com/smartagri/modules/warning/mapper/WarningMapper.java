package com.smartagri.modules.warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartagri.modules.warning.entity.WarningRecord;
import lombok.Data;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface WarningMapper extends BaseMapper<WarningRecord> {

    /**
     * 最新业务时间（触发时间优先，缺失时回退创建时间）。
     */
    @Select("SELECT MAX(COALESCE(trigger_time, create_time)) FROM warning")
    LocalDateTime selectLatestWarningTime();

    /**
     * 指定时间区间的预警汇总，避免把整表加载到内存后再统计。
     */
    @Select("""
            SELECT
                COUNT(*) AS total,
                COALESCE(SUM(level = 3), 0) AS high_risk,
                COALESCE(SUM(status IN (0, 1)), 0) AS pending,
                COALESCE(SUM(status = 3), 0) AS closed
            FROM warning
            WHERE COALESCE(trigger_time, create_time) >= #{startTime}
              AND COALESCE(trigger_time, create_time) < #{endTime}
            """)
    DailySummary selectDailySummary(@Param("startTime") LocalDateTime startTime,
                                    @Param("endTime") LocalDateTime endTime);

    /**
     * 区间内按天统计新增预警数量。
     */
    @Select("""
            SELECT DATE(COALESCE(trigger_time, create_time)) AS biz_date, COUNT(*) AS cnt
            FROM warning
            WHERE COALESCE(trigger_time, create_time) >= #{startTime}
              AND COALESCE(trigger_time, create_time) < #{endTime}
            GROUP BY DATE(COALESCE(trigger_time, create_time))
            """)
    List<DailyCount> countWarningsByDay(@Param("startTime") LocalDateTime startTime,
                                        @Param("endTime") LocalDateTime endTime);

    /**
     * 区间内按天统计闭环预警数量。
     */
    @Select("""
            SELECT DATE(close_time) AS biz_date, COUNT(*) AS cnt
            FROM warning
            WHERE status = 3
              AND close_time >= #{startTime}
              AND close_time < #{endTime}
            GROUP BY DATE(close_time)
            """)
    List<DailyCount> countClosedByDay(@Param("startTime") LocalDateTime startTime,
                                      @Param("endTime") LocalDateTime endTime);

    /**
     * 已闭环预警的平均闭环时长（分钟）。
     */
    @Select("""
            SELECT AVG(TIMESTAMPDIFF(MINUTE, trigger_time, close_time))
            FROM warning
            WHERE status = 3
              AND trigger_time IS NOT NULL
              AND close_time IS NOT NULL
            """)
    Double selectAvgClosureMinutes();

    @Data
    class DailyCount {
        private LocalDate bizDate;
        private Long cnt;
    }

    @Data
    class DailySummary {
        private Long total;
        private Long highRisk;
        private Long pending;
        private Long closed;
    }
}
