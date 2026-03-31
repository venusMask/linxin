package org.linxin.server.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.linxin.server.business.entity.Message;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    @Select("SELECT m.*, u.nickname as sender_nickname, u.avatar as sender_avatar " +
            "FROM messages m " +
            "LEFT JOIN users u ON m.sender_id = u.id " +
            "WHERE m.conversation_id = #{conversationId} AND m.deleted = 0 " +
            "ORDER BY m.send_time DESC")
    IPage<Message> selectMessagePage(Page<?> page, @Param("conversationId") Long conversationId);
}
