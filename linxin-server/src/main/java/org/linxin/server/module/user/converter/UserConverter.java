package org.linxin.server.module.user.converter;

import org.linxin.server.module.user.entity.User;
import org.linxin.server.module.user.vo.UserVO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserConverter {

    UserVO toVO(User user);
}
