package org.linxin.server.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.linxin.server.module.user.entity.User;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
