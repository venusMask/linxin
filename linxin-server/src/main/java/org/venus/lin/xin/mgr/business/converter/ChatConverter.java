package org.venus.lin.xin.mgr.business.converter;

import org.mapstruct.Mapper;
import org.venus.lin.xin.mgr.business.entity.Conversation;
import org.venus.lin.xin.mgr.business.entity.Message;
import org.venus.lin.xin.mgr.business.vo.ConversationVO;
import org.venus.lin.xin.mgr.business.vo.MessageVO;

@Mapper(componentModel = "spring")
public interface ChatConverter {

    ConversationVO toVO(Conversation conversation);

    MessageVO toVO(Message message);
}
