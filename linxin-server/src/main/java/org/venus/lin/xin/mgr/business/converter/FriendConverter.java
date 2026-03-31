package org.venus.lin.xin.mgr.business.converter;

import org.mapstruct.Mapper;
import org.venus.lin.xin.mgr.business.entity.FriendApply;
import org.venus.lin.xin.mgr.business.vo.FriendApplyVO;

@Mapper(componentModel = "spring")
public interface FriendConverter {

    FriendApplyVO toVO(FriendApply friendApply);
}
