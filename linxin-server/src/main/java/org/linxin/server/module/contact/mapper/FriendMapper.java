package org.linxin.server.module.contact.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.linxin.server.module.contact.entity.Friend;
import org.linxin.server.module.contact.vo.FriendVO;

@Mapper
public interface FriendMapper extends BaseMapper<Friend> {

    @Select("SELECT f.*, u_friend.username, u_friend.nickname, u_friend.avatar, u_friend.signature, u_friend.status as user_status "
            +
            "FROM friends f " +
            "JOIN users u_me ON f.user_id = u_me.id " +
            "LEFT JOIN users u_friend ON f.friend_id = u_friend.id " +
            "WHERE u_me.username = #{username} AND f.status = 1 AND f.deleted = 0 " +
            "ORDER BY f.update_time DESC")
    IPage<FriendVO> selectFriendPage(Page<?> page, @Param("username") String username);

    @Select("SELECT COUNT(*) FROM friends WHERE user_id = #{userId} AND friend_id = #{friendId} AND status = 1 AND deleted = 0")
    int isFriend(@Param("userId") Long userId, @Param("friendId") Long friendId);

    @Select("SELECT f.*, u.username, u.nickname, u.avatar, u.signature, u.status as user_status, u.user_type " +
            "FROM friends f " +
            "LEFT JOIN users u ON f.friend_id = u.id " +
            "WHERE f.user_id = #{userId} AND f.sequence_id > #{lastSequenceId} " +
            "ORDER BY f.sequence_id ASC")
    java.util.List<org.linxin.server.module.contact.vo.FriendVO> selectSyncRecords(@Param("userId") Long userId,
            @Param("lastSequenceId") Long lastSequenceId);

    @Select("SELECT * FROM friends WHERE user_id = #{userId}")
    java.util.List<org.linxin.server.module.contact.entity.Friend> selectAllRecordsByUserId(
            @Param("userId") Long userId);

    @Select("SELECT * FROM friends WHERE user_id = #{userId} AND friend_id = #{friendId} LIMIT 1")
    org.linxin.server.module.contact.entity.Friend selectByUserIdAndFriendId(@Param("userId") Long userId,
            @Param("friendId") Long friendId);

    @Select("SELECT MAX(sequence_id) FROM friends WHERE user_id = #{userId}")
    Long selectMaxSequenceId(@Param("userId") Long userId);
}
