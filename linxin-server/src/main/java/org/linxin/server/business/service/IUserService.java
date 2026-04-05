package org.linxin.server.business.service;

import java.util.List;
import org.linxin.server.business.entity.User;
import org.linxin.server.business.model.request.UpdateEmailRequest;
import org.linxin.server.business.model.request.UpdatePasswordRequest;
import org.linxin.server.business.model.request.UpdateProfileRequest;
import org.linxin.server.business.model.request.UserLoginRequest;
import org.linxin.server.business.model.request.UserRegisterRequest;
import org.linxin.server.business.vo.UserVO;

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
