import 'package:flutter/material.dart';
import 'package:lin_xin/modules/contact/friend.dart';
import 'package:lin_xin/core/state/data_service.dart';
import 'package:lin_xin/modules/contact/friend_service.dart';
import 'package:lin_xin/core/service/http_service.dart';
import 'package:lin_xin/widgets/avatar_widget.dart';
import 'package:lin_xin/modules/chat/pages/chat_detail_page.dart';

class UserDetailsPage extends StatefulWidget {
  final Friend friend;
  final String? heroTag;

  const UserDetailsPage({
    super.key,
    required this.friend,
    this.heroTag,
  });

  @override
  State<UserDetailsPage> createState() => _UserDetailsPageState();
}

class _UserDetailsPageState extends State<UserDetailsPage> {
  late Friend _currentFriend;

  @override
  void initState() {
    super.initState();
    _currentFriend = widget.friend;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        foregroundColor: Colors.black,
      ),
      extendBodyBehindAppBar: true,
      body: SingleChildScrollView(
        child: Column(
          children: [
            _buildHeader(),
            const SizedBox(height: 24),
            _buildInfoList(),
            const SizedBox(height: 60), // 代替 Spacer，提供一定间距
            _buildActionButtons(context),
            const SizedBox(height: 40),
          ],
        ),
      ),
    );
  }

  Widget _buildHeader() {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.only(top: 100, bottom: 40),
      decoration: BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          colors: [
            Colors.green[50]!,
            Colors.white,
          ],
        ),
      ),
      child: Column(
        children: [
          Hero(
            tag: widget.heroTag ?? 'avatar_${_currentFriend.id}',
            child: AvatarWidget(
              imageUrl: _currentFriend.avatar,
              name: _currentFriend.name,
              size: 100,
            ),
          ),
          const SizedBox(height: 16),
          Text(
            _currentFriend.name,
            style: const TextStyle(
              fontSize: 24,
              fontWeight: FontWeight.bold,
              color: Colors.black87,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            '灵信号: ${_currentFriend.username ?? _currentFriend.id}',
            style: TextStyle(
              fontSize: 14,
              color: Colors.grey[600],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildInfoList() {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 24),
      child: Column(
        children: [
          _buildInfoItem('地区', '未设置'),
          const Divider(height: 32),
          _buildTagsItem(),
          const Divider(height: 32),
          _buildInfoItem('个人签名', '未设置'),
        ],
      ),
    );
  }

  Widget _buildTagsItem() {
    return InkWell(
      onTap: _showEditTagsDialog,
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 4),
        child: Row(
          children: [
            const SizedBox(
              width: 80,
              child: Text(
                '标签',
                style: TextStyle(fontSize: 16, color: Colors.black87),
              ),
            ),
            Expanded(
              child: _currentFriend.tags.isEmpty
                  ? Text('设置标签', style: TextStyle(color: Colors.grey[400], fontSize: 16))
                  : Wrap(
                      spacing: 8,
                      runSpacing: 4,
                      children: _currentFriend.tags.map((tag) => _buildTagChip(tag)).toList(),
                    ),
            ),
            Icon(Icons.chevron_right, color: Colors.grey[400], size: 20),
          ],
        ),
      ),
    );
  }

  Widget _buildTagChip(String tag) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 2),
      decoration: BoxDecoration(
        color: Colors.green[50],
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: Colors.green[100]!),
      ),
      child: Text(
        tag,
        style: TextStyle(color: Colors.green[700], fontSize: 13),
      ),
    );
  }

  Future<void> _showEditTagsDialog() async {
    final List<String> presets = ['家人', '亲戚', '爱人', '媳妇', '老公', '同学', '同事', '朋友', '医生', '老板'];
    final controller = TextEditingController(text: _currentFriend.tags.join(', '));
    
    final result = await showDialog<String>(
      context: context,
      builder: (context) => StatefulBuilder(
        builder: (context, setDialogState) {
          void togglePreset(String preset) {
            List<String> current = controller.text
                .split(RegExp(r'[,，]'))
                .map((e) => e.trim())
                .where((e) => e.isNotEmpty)
                .toList();
            if (current.contains(preset)) {
              current.remove(preset);
            } else {
              current.add(preset);
            }
            controller.text = current.join(', ');
            setDialogState(() {});
          }

          return AlertDialog(
            title: const Text('编辑标签'),
            content: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                TextField(
                  controller: controller,
                  decoration: const InputDecoration(
                    hintText: '多个标签请用逗号分隔',
                    helperText: '手动输入或从下方选择',
                  ),
                  autofocus: true,
                ),
                const SizedBox(height: 16),
                const Text('常用预设:', style: TextStyle(fontSize: 12, color: Colors.grey)),
                const SizedBox(height: 8),
                Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: presets.map((preset) {
                    bool isSelected = controller.text.contains(preset);
                    return ChoiceChip(
                      label: Text(preset, style: const TextStyle(fontSize: 12)),
                      selected: isSelected,
                      onSelected: (_) => togglePreset(preset),
                      selectedColor: Colors.green[100],
                      labelStyle: TextStyle(color: isSelected ? Colors.green[700] : Colors.black87),
                    );
                  }).toList(),
                ),
              ],
            ),
            actions: [
              TextButton(onPressed: () => Navigator.pop(context), child: const Text('取消')),
              TextButton(
                onPressed: () => Navigator.pop(context, controller.text),
                child: const Text('保存'),
              ),
            ],
          );
        }
      ),
    );

    if (result != null) {
      _updateTags(result);
    }
  }

  Future<void> _updateTags(String tagsString) async {
    final List<String> newTags = tagsString
        .split(RegExp(r'[,，]'))
        .map((e) => e.trim())
        .where((e) => e.isNotEmpty)
        .toList();

    try {
      await HttpService().post('/friends/update', data: {
        'friendId': _currentFriend.id,
        'tags': newTags.join(','),
      });

      setState(() {
        _currentFriend = _currentFriend.copyWith(tags: newTags);
      });

      // 刷新全局数据
      DataService().refreshFriends();

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('标签更新成功')));
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('更新失败: $e')));
      }
    }
  }

  Widget _buildInfoItem(String label, String value) {
    return Row(
      children: [
        SizedBox(
          width: 80,
          child: Text(
            label,
            style: const TextStyle(
              fontSize: 16,
              color: Colors.black87,
            ),
          ),
        ),
        Expanded(
          child: Text(
            value,
            style: TextStyle(
              fontSize: 16,
              color: Colors.grey[600],
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildActionButtons(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 24),
      child: Column(
        children: [
          ElevatedButton(
            onPressed: () {
              final chat = DataService().createChat(_currentFriend);
              Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (context) => ChatDetailPage(chat: chat),
                ),
              );
            },
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.green[600],
              foregroundColor: Colors.white,
              minimumSize: const Size(double.infinity, 54),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(12),
              ),
              elevation: 0,
            ),
            child: const Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(Icons.chat_bubble_outline, size: 20),
                SizedBox(width: 8),
                Text(
                  '发信息',
                  style: TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),
          if (_currentFriend.userType != 1)
            OutlinedButton(
              onPressed: () => _showDeleteConfirm(context),
              style: OutlinedButton.styleFrom(
                foregroundColor: Colors.red[600],
                side: BorderSide(color: Colors.red[100]!),
                minimumSize: const Size(double.infinity, 54),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
              ),
              child: const Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(Icons.person_remove_outlined, size: 20),
                  SizedBox(width: 8),
                  Text(
                    '删除好友',
                    style: TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                ],
              ),
            ),
        ],
      ),
    );
  }

  void _showDeleteConfirm(BuildContext context) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('删除好友'),
        content: Text('确定要删除好友 "${_currentFriend.name}" 吗？此操作不可撤销。'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('取消'),
          ),
          TextButton(
            onPressed: () async {
              try {
                await FriendService.instance.deleteFriend(_currentFriend.id);
                await DataService().refreshFriends();
                
                if (context.mounted) {
                  Navigator.pop(context); // Close dialog
                  Navigator.pop(context); // Back to friend list
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('已删除好友')),
                  );
                }
              } catch (e) {
                if (context.mounted) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(content: Text('删除失败: $e')),
                  );
                }
              }
            },
            child: const Text('删除', style: TextStyle(color: Colors.red)),
          ),
        ],
      ),
    );
  }
}
