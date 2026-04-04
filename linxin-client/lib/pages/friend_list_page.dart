import 'package:flutter/material.dart';
import 'package:flutter/scheduler.dart';
import 'package:provider/provider.dart';
import '../services/http_service.dart';
import '../models/user.dart';
import '../models/friend.dart';
import '../widgets/avatar_widget.dart';
import '../services/data_service.dart';
import 'friend_apply_list_page.dart';
import 'user_search_result_page.dart';
import 'user_details_page.dart';

class FriendListPage extends StatefulWidget {
  const FriendListPage({super.key});

  @override
  State<FriendListPage> createState() => _FriendListPageState();
}

class _FriendListPageState extends State<FriendListPage> {
  final HttpService _httpService = HttpService();

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final dataService = context.read<DataService>();
      dataService.refreshFriends();
      dataService.refreshPendingApplyCount();
    });
  }

  // _loadFriends removed as it is now moved to DataService

  void _showSearchDialog() {
    final TextEditingController searchController = TextEditingController();
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('添加好友'),
        content: TextField(
          controller: searchController,
          decoration: const InputDecoration(
            hintText: '输入好友账号 (username)',
            suffixIcon: Icon(Icons.search),
          ),
          autofocus: true,
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('取消'),
          ),
          TextButton(
            onPressed: () async {
              final keyword = searchController.text.trim();
              if (keyword.isEmpty) return;
              
              try {
                final results = await _httpService.searchUsers(keyword);
                if (!mounted) return;
                
                SchedulerBinding.instance.addPostFrameCallback((_) {
                  if (!mounted) return;
                  Navigator.pop(context); // 关闭弹窗
                  if (results.isNotEmpty) {
                    final user = User.fromJson(results.first);
                    Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (context) => UserSearchResultPage(user: user),
                      ),
                    );
                  } else {
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(content: Text('用户不存在')),
                    );
                  }
                });
              } catch (e) {
                if (!mounted) return;
                
                SchedulerBinding.instance.addPostFrameCallback((_) {
                  if (!mounted) return;
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(content: Text('搜索失败: $e')),
                  );
                });
              }
            },
            child: const Text('搜索'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final dataService = context.watch<DataService>();
    final friends = dataService.friends;

    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        title: const Text(
          '通讯录',
          style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18),
        ),
        actions: [
          IconButton(
            icon: Icon(Icons.person_add_alt_1_outlined, color: Colors.green[600]),
            onPressed: _showSearchDialog,
          ),
        ],
        backgroundColor: Colors.white,
        foregroundColor: Colors.black87,
        elevation: 0.5,
        centerTitle: true,
      ),
      body: ListView.builder(
        itemCount: friends.length + 1, // +1 for "New Friends"
        itemBuilder: (context, index) {
          if (index == 0) {
            return _buildNewFriendItem(dataService.pendingApplyCount);
          }
          final friend = friends[index - 1];
          return _buildFriendItem(friend);
        },
      ),
    );
  }

  Widget _buildNewFriendItem(int pendingCount) {
    return ListTile(
      leading: Stack(
        children: [
          Container(
            width: 48,
            height: 48,
            decoration: BoxDecoration(
              color: Colors.orange[400],
              borderRadius: BorderRadius.circular(8),
            ),
            child: const Icon(Icons.person_add, color: Colors.white),
          ),
          if (pendingCount > 0)
            Positioned(
              right: 0,
              top: 0,
              child: Container(
                padding: const EdgeInsets.all(4),
                decoration: const BoxDecoration(
                  color: Colors.red,
                  shape: BoxShape.circle,
                ),
                constraints: const BoxConstraints(
                  minWidth: 16,
                  minHeight: 16,
                ),
                child: Text(
                  pendingCount > 99 ? '99+' : '$pendingCount',
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 10,
                    fontWeight: FontWeight.bold,
                  ),
                  textAlign: TextAlign.center,
                ),
              ),
            ),
        ],
      ),
      title: const Text(
        '新的朋友',
        style: TextStyle(fontWeight: FontWeight.w500),
      ),
      onTap: () {
        Navigator.push(
          context,
          MaterialPageRoute(
            builder: (context) => const FriendApplyListPage(),
          ),
        );
      },
    );
  }

  Widget _buildFriendItem(Friend friend) {
    return InkWell(
      onTap: () async {
        final heroTag = 'friend_avatar_${friend.id}';
        await Navigator.push(
          context,
          MaterialPageRoute(
            builder: (context) => UserDetailsPage(friend: friend, heroTag: heroTag),
          ),
        );
      },
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        child: Row(
          children: [
            Hero(
              tag: 'friend_avatar_${friend.id}',
              child: AvatarWidget(
                imageUrl: friend.avatar,
                name: friend.name,
              ),
            ),
            const SizedBox(width: 14),
            Text(
              friend.name,
              style: const TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.w500,
                color: Colors.black87,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
