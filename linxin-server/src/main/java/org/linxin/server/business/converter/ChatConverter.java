package org.linxin.server.business.converter;

import org.linxin.server.business.entity.Conversation;
import org.linxin.server.business.entity.Message;
import org.linxin.server.business.vo.ConversationVO;
import org.linxin.server.business.vo.MessageVO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ChatConverter {

    ConversationVO toVO(Conversation conversation);

    MessageVO toVO(Message message);
}
