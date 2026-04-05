package org.linxin.server.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.linxin.server.ai.entity.AIUsageLog;

@Mapper
public interface AIUsageLogMapper extends BaseMapper<AIUsageLog> {

    @Select("SELECT DATE(create_time) as date, SUM(total_tokens) as totalTokens " +
            "FROM ai_usage_logs " +
            "WHERE user_id = #{userId} AND create_time >= #{startTime} " +
            "GROUP BY DATE(create_time) " +
            "ORDER BY date ASC")
    List<Map<String, Object>> selectDailyUsage(@Param("userId") Long userId,
            @Param("startTime") LocalDateTime startTime);

    @Select("SELECT intent, SUM(total_tokens) as totalTokens " +
            "FROM ai_usage_logs " +
            "WHERE user_id = #{userId} AND create_time >= #{startTime} " +
            "GROUP BY intent")
    List<Map<String, Object>> selectIntentUsage(@Param("userId") Long userId,
            @Param("startTime") LocalDateTime startTime);
}
