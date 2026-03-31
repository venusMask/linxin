package org.venus.lin.xin.mgr.business.service;

import org.venus.lin.xin.mgr.business.entity.User;
import org.venus.lin.xin.mgr.business.model.request.UserLoginRequest;
import org.venus.lin.xin.mgr.business.model.request.UserRegisterRequest;
import org.venus.lin.xin.mgr.business.vo.UserVO;

import java.util.List;

public interface IUserService {

    User register(UserRegisterRequest request);

    User login(UserLoginRequest request);

    UserVO getUserInfo(Long userId);

    UserVO getUserById(Long userId);

    UserVO getUserByUsername(String username);

    List<UserVO> searchUsers(String keyword);
}
