import 'package:flutter/material.dart';
import '../services/http_service.dart';
import '../services/data_service.dart';
import '../models/user.dart';
import '../models/friend.dart';
import '../config/api_config.dart';
import '../widgets/avatar_widget.dart';
import '../services/auth_service.dart';
import 'friend_apply_list_page.dart';
import 'user_search_result_page.dart';
import 'user_details_page.dart';

class FriendListPage extends StatefulWidget {
  const FriendListPage({super.key});

  @override
  State<FriendListPage> createState() => _FriendListPageState();
}

class _FriendListPageState extends State<FriendListPage> {
  final DataService _dataService = DataService();
  final HttpService _httpService = HttpService();
  List<Friend> _friends = [];

  @override
  void initState() {
    super.initState();
    _loadFriends();
  }

  void _loadFriends() async {
    try {
      final response = await _httpService.post(
        ApiConfig.friendList,
        data: {
          'username': AuthService().currentUser?.username,
          'pageNum': 1,
          'pageSize': 100, // 获取全部好友或较多数量
        },
      );
      // 后端返回的是 IPage<FriendVO>，结构为 {records: [...], total: ...}
      final List<dynamic> records = response.data['records'] ?? [];
      
      if (mounted) {
        setState(() {
          _friends = records.map((json) => Friend(
            id: json['friendId']?.toString() ?? '',
            name: json['friendNickname'] ?? json['nickname'] ?? '未知用户',
            avatar: json['avatar'] ?? '',
          )).toList();
        });
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('加载好友列表失败: $e')),
        );
      }
    }
  }

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
                if (mounted) {
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
                }
              } catch (e) {
                if (mounted) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(content: Text('搜索失败: $e')),
                  );
                }
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
        itemCount: _friends.length + 1, // +1 for "New Friends"
        itemBuilder: (context, index) {
          if (index == 0) {
            return _buildNewFriendItem();
          }
          final friend = _friends[index - 1];
          return _buildFriendItem(friend);
        },
      ),
    );
  }

  Widget _buildNewFriendItem() {
    return ListTile(
      leading: Container(
        width: 48,
        height: 48,
        decoration: BoxDecoration(
          color: Colors.orange[400],
          borderRadius: BorderRadius.circular(8),
        ),
        child: const Icon(Icons.person_add, color: Colors.white),
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
        await Navigator.push(
          context,
          MaterialPageRoute(
            builder: (context) => UserDetailsPage(friend: friend),
          ),
        );
        _loadFriends();
      },
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        child: Row(
          children: [
            Hero(
              tag: 'avatar_${friend.id}',
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
