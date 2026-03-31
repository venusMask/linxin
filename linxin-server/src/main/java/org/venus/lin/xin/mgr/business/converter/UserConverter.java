package org.venus.lin.xin.mgr.business.converter;

import org.mapstruct.Mapper;
import org.venus.lin.xin.mgr.business.entity.User;
import org.venus.lin.xin.mgr.business.vo.UserVO;

@Mapper(componentModel = "spring")
public interface UserConverter {

    UserVO toVO(User user);
}
