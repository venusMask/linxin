package org.linxin.server.module.user.service;

import java.util.List;
import org.linxin.server.module.user.entity.User;
import org.linxin.server.module.user.model.request.UpdateEmailRequest;
import org.linxin.server.module.user.model.request.UpdatePasswordRequest;
import org.linxin.server.module.user.model.request.UpdateProfileRequest;
import org.linxin.server.module.user.model.request.UserLoginRequest;
import org.linxin.server.module.user.model.request.UserRegisterRequest;
import org.linxin.server.module.user.vo.UserVO;

public interface IUserService {

    User register(UserRegisterRequest request);

    User login(UserLoginRequest request);

    UserVO getUserInfo(Long userId);

    UserVO getUserById(Long userId);

    UserVO getUserByUsername(String username);

    List<UserVO> searchUsers(String keyword);

    void updateProfile(Long userId, UpdateProfileRequest request);

    void updateEmail(Long userId, UpdateEmailRequest request);

    void updatePassword(Long userId, UpdatePasswordRequest request);
}
