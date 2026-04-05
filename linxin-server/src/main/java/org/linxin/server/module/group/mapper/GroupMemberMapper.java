package org.linxin.server.module.group.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.linxin.server.module.group.entity.GroupMember;

@Mapper
public interface GroupMemberMapper extends BaseMapper<GroupMember> {
}
