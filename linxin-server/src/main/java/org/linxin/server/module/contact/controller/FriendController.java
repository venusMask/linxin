package org.linxin.server.module.contact.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.linxin.server.common.result.Result;
import org.linxin.server.module.contact.model.request.FriendApplyRequest;
import org.linxin.server.module.contact.model.request.FriendHandleRequest;
import org.linxin.server.module.contact.model.request.FriendListRequest;
import org.linxin.server.module.contact.model.request.FriendUpdateRequest;
import org.linxin.server.module.contact.service.IFriendService;
import org.linxin.server.module.contact.vo.FriendApplyVO;
import org.linxin.server.module.contact.vo.FriendVO;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/friends")
@RequiredArgsConstructor
@Tag(name = "好友管理模块", description = "好友相关操作接口")
public class FriendController {

    private final IFriendService friendService;

    @PostMapping("/list")
    @Operation(summary = "获取好友列表")
    public Result<IPage<FriendVO>> getFriendList(
            @RequestAttribute("userId") Long userId,
            @RequestBody FriendListRequest request) {
        IPage<FriendVO> friends = friendService.getFriendList(
                userId,
                request.getUsername(),
                request.getPageNum(),
                request.getPageSize());
        return Result.success(friends);
    }

    @GetMapping("/sync")
    @Operation(summary = "增量同步好友列表")
    public Result<List<FriendVO>> syncFriends(
            @RequestAttribute("userId") Long userId,
            @RequestParam(defaultValue = "0") Long lastSequenceId) {
        List<FriendVO> friends = friendService.syncFriends(userId, lastSequenceId);
        return Result.success(friends);
    }

    @PostMapping("/apply")
    @Operation(summary = "发送好友申请")
    public Result<String> applyAddFriend(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody FriendApplyRequest request) {
        friendService.applyAddFriend(userId, request);
        return Result.success("申请已发送");
    }

    @GetMapping("/apply/received")
    @Operation(summary = "获取收到的好友申请列表")
    public Result<List<FriendApplyVO>> getReceivedApplyList(@RequestAttribute("userId") Long userId) {
        List<FriendApplyVO> applies = friendService.getReceivedApplyList(userId);
        return Result.success(applies);
    }

    @GetMapping("/apply/sent")
    @Operation(summary = "获取发出的好友申请列表")
    public Result<List<FriendApplyVO>> getSentApplyList(@RequestAttribute("userId") Long userId) {
        List<FriendApplyVO> applies = friendService.getSentApplyList(userId);
        return Result.success(applies);
    }

    @PostMapping("/apply/handle")
    @Operation(summary = "处理好友申请")
    public Result<String> handleFriendApply(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody FriendHandleRequest request) {
        friendService.handleFriendApply(userId, request);
        return Result.success("处理成功");
    }

    @PutMapping("/update")
    @Operation(summary = "更新好友信息")
    public Result<String> updateFriend(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody FriendUpdateRequest request) {
        friendService.updateFriend(userId, request);
        return Result.success("更新成功");
    }

    @DeleteMapping("/{friendId}")
    @Operation(summary = "删除好友")
    public Result<String> deleteFriend(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long friendId) {
        friendService.deleteFriend(userId, friendId);
        return Result.success("删除成功");
    }

    @GetMapping("/check/{friendId}")
    @Operation(summary = "检查是否是好有关系")
    public Result<Boolean> isFriend(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long friendId) {
        boolean result = friendService.isFriend(userId, friendId);
        return Result.success(result);
    }
}
