package org.linxin.server.business.converter;

import org.mapstruct.Mapper;
import org.linxin.server.business.entity.User;
import org.linxin.server.business.vo.UserVO;

@Mapper(componentModel = "spring")
public interface UserConverter {

    UserVO toVO(User user);
}
