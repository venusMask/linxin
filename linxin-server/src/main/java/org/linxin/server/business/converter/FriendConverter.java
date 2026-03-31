package org.linxin.server.business.converter;

import org.mapstruct.Mapper;
import org.linxin.server.business.entity.FriendApply;
import org.linxin.server.business.vo.FriendApplyVO;

@Mapper(componentModel = "spring")
public interface FriendConverter {

    FriendApplyVO toVO(FriendApply friendApply);
}
