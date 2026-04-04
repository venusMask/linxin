package org.linxin.server.business.converter;

import org.linxin.server.business.entity.User;
import org.linxin.server.business.vo.UserVO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserConverter {

    UserVO toVO(User user);
}
