package org.linxin.server.business.converter;

import org.linxin.server.business.entity.FriendApply;
import org.linxin.server.business.vo.FriendApplyVO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FriendConverter {

    FriendApplyVO toVO(FriendApply friendApply);
}
