package org.linxin.server.module.chat.converter;

import org.linxin.server.module.chat.entity.Conversation;
import org.linxin.server.module.chat.entity.Message;
import org.linxin.server.module.chat.vo.ConversationVO;
import org.linxin.server.module.chat.vo.MessageVO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ChatConverter {

    ConversationVO toVO(Conversation conversation);

    MessageVO toVO(Message message);
}
