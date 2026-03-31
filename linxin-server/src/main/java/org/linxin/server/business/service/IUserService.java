package org.linxin.server.business.service;

import org.linxin.server.business.entity.User;
import org.linxin.server.business.model.request.UserLoginRequest;
import org.linxin.server.business.model.request.UserRegisterRequest;
import org.linxin.server.business.vo.UserVO;

import java.util.List;

public interface IUserService {

    User register(UserRegisterRequest request);

    User login(UserLoginRequest request);

    UserVO getUserInfo(Long userId);

    UserVO getUserById(Long userId);

    UserVO getUserByUsername(String username);

    List<UserVO> searchUsers(String keyword);
}
