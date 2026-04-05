package org.linxin.server.module.contact.converter;

import org.linxin.server.module.contact.entity.FriendApply;
import org.linxin.server.module.contact.vo.FriendApplyVO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FriendConverter {

    FriendApplyVO toVO(FriendApply friendApply);
}
