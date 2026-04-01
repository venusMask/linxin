package org.linxin.server.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.linxin.server.business.entity.Message;
import org.linxin.server.business.vo.MessageVO;

import java.util.List;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    @Select("SELECT m.*, u.nickname as sender_nickname, u.avatar as sender_avatar " +
            "FROM message_status ms " +
            "JOIN messages m ON ms.message_id = m.id " +
            "LEFT JOIN users u ON m.sender_id = u.id " +
            "WHERE ms.user_id = #{userId} AND ms.read_status = 0 AND ms.deleted = 0 " +
            "ORDER BY m.send_time ASC")
    List<MessageVO> selectUnreadMessages(@Param("userId") Long userId);
}
