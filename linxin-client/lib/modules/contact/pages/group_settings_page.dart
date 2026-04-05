import 'package:flutter/material.dart';
import 'package:flutter/scheduler.dart';
import 'package:lin_xin/modules/contact/group.dart';
import 'package:lin_xin/modules/contact/group_member.dart';
import 'package:lin_xin/modules/contact/group_service.dart';
import 'package:lin_xin/modules/auth/auth_service.dart';
import 'package:lin_xin/widgets/avatar_widget.dart';

class GroupSettingsPage extends StatefulWidget {
  final Group group;

  const GroupSettingsPage({super.key, required this.group});

  @override
  State<GroupSettingsPage> createState() => _GroupSettingsPageState();
}

class _GroupSettingsPageState extends State<GroupSettingsPage> {
  final GroupService _groupService = GroupService.instance;
  List<GroupMember> _members = [];
  bool _isLoading = true;
  bool _isOwner = false;

  @override
  void initState() {
    super.initState();
    _loadMembers();
    _checkOwner();
  }

  void _checkOwner() {
    final currentUserId = AuthService().currentUser?.id;
    setState(() {
      _isOwner = widget.group.ownerId.toString() == currentUserId?.toString();
    });
  }

  void _loadMembers() async {
    try {
      final members = await _groupService.getGroupMembers(widget.group.id);
      if (mounted) {
        setState(() {
          _members = members;
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('加载群成员失败: $e')),
        );
      }
    }
  }

  void _showAnnouncementDialog() {
    final TextEditingController controller = TextEditingController(
      text: widget.group.announcement ?? '',
    );
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('群公告'),
        content: TextField(
          controller: controller,
          maxLines: 5,
          decoration: const InputDecoration(
            hintText: '输入群公告内容',
            border: OutlineInputBorder(),
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('取消'),
          ),
          TextButton(
            onPressed: () async {
              try {
                await _groupService.updateAnnouncement(
                  groupId: widget.group.id,
                  announcement: controller.text,
                );
                if (!mounted) return;
                
                SchedulerBinding.instance.addPostFrameCallback((_) {
                  if (!mounted) return;
                  Navigator.pop(context);
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('群公告已更新')),
                  );
                });
              } catch (e) {
                if (!mounted) return;
                
                SchedulerBinding.instance.addPostFrameCallback((_) {
                  if (!mounted) return;
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(content: Text('更新失败: $e')),
                  );
                });
              }
            },
            child: const Text('保存'),
          ),
        ],
      ),
    );
  }

  void _showAddMembersDialog() {
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => AddGroupMembersPage(groupId: widget.group.id),
      ),
    ).then((_) => _loadMembers());
  }

  void _showRemoveMemberDialog(GroupMember member) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('移除成员'),
        content: Text('确定要将 ${member.nickname} 移出群聊吗？'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('取消'),
          ),
          TextButton(
            onPressed: () async {
              try {
                await _groupService.removeMember(
                  groupId: widget.group.id,
                  memberId: member.userId,
                );
                if (!mounted) return;
                
                SchedulerBinding.instance.addPostFrameCallback((_) {
                  if (!mounted) return;
                  Navigator.pop(context);
                  _loadMembers();
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('成员已移除')),
                  );
                });
              } catch (e) {
                if (!mounted) return;
                
                SchedulerBinding.instance.addPostFrameCallback((_) {
                  if (!mounted) return;
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(content: Text('移除失败: $e')),
                  );
                });
              }
            },
            child: const Text('确定'),
          ),
        ],
      ),
    );
  }

  void _leaveGroup() {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('退出群聊'),
        content: const Text('确定要退出该群聊吗？'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('取消'),
          ),
          TextButton(
            onPressed: () async {
              try {
                await _groupService.leaveGroup(widget.group.id);
                if (!mounted) return;
                
                SchedulerBinding.instance.addPostFrameCallback((_) {
                  if (!mounted) return;
                  Navigator.pop(context);
                  Navigator.pop(context);
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('已退出群聊')),
                  );
                });
              } catch (e) {
                if (!mounted) return;
                
                SchedulerBinding.instance.addPostFrameCallback((_) {
                  if (!mounted) return;
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(content: Text('退出失败: $e')),
                  );
                });
              }
            },
            child: const Text('确定'),
          ),
        ],
      ),
    );
  }

  void _dissolveGroup() {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('解散群聊'),
        content: const Text('确定要解散该群聊吗？此操作不可恢复！'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('取消'),
          ),
          TextButton(
            onPressed: () async {
              try {
                await _groupService.dissolveGroup(widget.group.id);
                if (!mounted) return;
                
                SchedulerBinding.instance.addPostFrameCallback((_) {
                  if (!mounted) return;
                  Navigator.pop(context);
                  Navigator.pop(context);
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('群聊已解散')),
                  );
                });
              } catch (e) {
                if (!mounted) return;
                
                SchedulerBinding.instance.addPostFrameCallback((_) {
                  if (!mounted) return;
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(content: Text('解散失败: $e')),
                  );
                });
              }
            },
            child: const Text('确定'),
          ),
        ],
      ),
    );
  }

  String _getRoleName(int role) {
    switch (role) {
      case 2:
        return '群主';
      case 1:
        return '管理员';
      default:
        return '成员';
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        title: const Text(
          '群设置',
          style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18),
        ),
        backgroundColor: Colors.white,
        foregroundColor: Colors.black87,
        elevation: 0.5,
        centerTitle: true,
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : ListView(
              children: [
                _buildGroupInfo(),
                const Divider(),
                _buildAnnouncement(),
                const Divider(),
                _buildMembers(),
                const Divider(),
                _buildActions(),
              ],
            ),
    );
  }

  Widget _buildGroupInfo() {
    return Container(
      padding: const EdgeInsets.all(16),
      child: Row(
        children: [
          Hero(
            tag: 'group_avatar_${widget.group.id}',
            child: AvatarWidget(
              imageUrl: widget.group.avatar,
              name: widget.group.name,
              size: 60,
            ),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  widget.group.name,
                  style: const TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  '群成员 ${_members.length}/${widget.group.memberLimit}',
                  style: TextStyle(
                    fontSize: 14,
                    color: Colors.grey[600],
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildAnnouncement() {
    return ListTile(
      leading: Icon(Icons.campaign, color: Colors.green[600]),
      title: const Text('群公告'),
      subtitle: Text(
        widget.group.announcement ?? '暂无公告',
        maxLines: 1,
        overflow: TextOverflow.ellipsis,
      ),
      trailing: const Icon(Icons.chevron_right),
      onTap: _isOwner ? _showAnnouncementDialog : null,
    );
  }

  Widget _buildMembers() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                '群成员 (${_members.length})',
                style: const TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                ),
              ),
              if (_isOwner)
                TextButton.icon(
                  onPressed: _showAddMembersDialog,
                  icon: const Icon(Icons.person_add),
                  label: const Text('添加'),
                ),
            ],
          ),
        ),
        ListView.builder(
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          itemCount: _members.length,
          itemBuilder: (context, index) {
            final member = _members[index];
            return _buildMemberItem(member);
          },
        ),
      ],
    );
  }

  Widget _buildMemberItem(GroupMember member) {
    final currentUserId = AuthService().currentUser?.id?.toString();
    final isSelf = member.userId == currentUserId;

    return ListTile(
      leading: AvatarWidget(
        imageUrl: member.avatar,
        name: member.nickname,
      ),
      title: Row(
        children: [
          Text(member.nickname),
          if (isSelf)
            Text(
              ' (我)',
              style: TextStyle(color: Colors.grey[600], fontSize: 14),
            ),
          if (member.isOwner)
            Container(
              margin: const EdgeInsets.only(left: 8),
              padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
              decoration: BoxDecoration(
                color: Colors.orange[100],
                borderRadius: BorderRadius.circular(4),
              ),
              child: Text(
                '群主',
                style: TextStyle(color: Colors.orange[800], fontSize: 10),
              ),
            ),
          if (member.isAdmin)
            Container(
              margin: const EdgeInsets.only(left: 8),
              padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
              decoration: BoxDecoration(
                color: Colors.blue[100],
                borderRadius: BorderRadius.circular(4),
              ),
              child: Text(
                '管理员',
                style: TextStyle(color: Colors.blue[800], fontSize: 10),
              ),
            ),
        ],
      ),
      subtitle: Text(_getRoleName(member.role)),
      trailing: _isOwner && !member.isOwner && !isSelf
          ? IconButton(
              icon: const Icon(Icons.remove_circle_outline, color: Colors.red),
              onPressed: () => _showRemoveMemberDialog(member),
            )
          : null,
    );
  }

  Widget _buildActions() {
    return Padding(
      padding: const EdgeInsets.all(16),
      child: Column(
        children: [
          SizedBox(
            width: double.infinity,
            child: OutlinedButton(
              onPressed: _leaveGroup,
              style: OutlinedButton.styleFrom(
                foregroundColor: Colors.red,
                side: const BorderSide(color: Colors.red),
              ),
              child: const Text('退出群聊'),
            ),
          ),
          if (_isOwner) ...[
            const SizedBox(height: 12),
            SizedBox(
              width: double.infinity,
              child: ElevatedButton(
                onPressed: _dissolveGroup,
                style: ElevatedButton.styleFrom(
                  backgroundColor: Colors.red,
                  foregroundColor: Colors.white,
                ),
                child: const Text('解散群聊'),
              ),
            ),
          ],
        ],
      ),
    );
  }
}

class AddGroupMembersPage extends StatefulWidget {
  final String groupId;

  const AddGroupMembersPage({super.key, required this.groupId});

  @override
  State<AddGroupMembersPage> createState() => _AddGroupMembersPageState();
}

class _AddGroupMembersPageState extends State<AddGroupMembersPage> {
  final GroupService _groupService = GroupService.instance;
  final Set<String> _selectedIds = {};

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('添加群成员'),
        actions: [
          TextButton(
            onPressed: _selectedIds.isEmpty
                ? null
                : () async {
                    try {
                      await _groupService.addMembers(
                        groupId: widget.groupId,
                        memberIds: _selectedIds.toList(),
                      );
                      if (!mounted) return;
                      
                      SchedulerBinding.instance.addPostFrameCallback((_) {
                        if (!mounted) return;
                        Navigator.pop(context);
                        ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(content: Text('成员已添加')),
                        );
                      });
                    } catch (e) {
                      if (!mounted) return;
                      
                      SchedulerBinding.instance.addPostFrameCallback((_) {
                        if (!mounted) return;
                        ScaffoldMessenger.of(context).showSnackBar(
                          SnackBar(content: Text('添加失败: $e')),
                        );
                      });
                    }
                  },
            child: Text(
              '添加 (${_selectedIds.length})',
              style: TextStyle(
                color: _selectedIds.isEmpty ? Colors.grey : Colors.green[600],
              ),
            ),
          ),
        ],
      ),
      body: const Center(
        child: Text('从好友列表选择成员的UI待实现'),
      ),
    );
  }
}
