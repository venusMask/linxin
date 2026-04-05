import 'package:flutter/material.dart';
import 'package:lin_xin/core/service/http_service.dart';
import 'package:lin_xin/widgets/avatar_widget.dart';

class FriendApplyListPage extends StatefulWidget {
  const FriendApplyListPage({super.key});

  @override
  State<FriendApplyListPage> createState() => _FriendApplyListPageState();
}

class _FriendApplyListPageState extends State<FriendApplyListPage> {
  final HttpService _httpService = HttpService();
  List<dynamic> _applies = [];
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadApplies();
  }

  Future<void> _loadApplies() async {
    setState(() {
      _isLoading = true;
    });

    try {
      final results = await _httpService.getReceivedApplies();
      if (mounted) {
        setState(() {
          _applies = results;
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('获取数据失败: $e')),
        );
      }
    }
  }

  Future<void> _handleApply(dynamic applyId, int status) async {
    try {
      await _httpService.handleFriendApply(applyId, status);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('处理成功')),
        );
        _loadApplies(); // 刷新列表
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('处理失败: $e')),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        title: const Text('新的朋友'),
        backgroundColor: Colors.white,
        foregroundColor: Colors.black,
        elevation: 0.5,
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _applies.isEmpty
              ? _buildEmptyView()
              : ListView.separated(
                  itemCount: _applies.length,
                  separatorBuilder: (context, index) => const Divider(height: 1),
                  itemBuilder: (context, index) {
                    final apply = _applies[index];
                    return _buildApplyItem(apply);
                  },
                ),
    );
  }

  Widget _buildEmptyView() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.person_outline, size: 80, color: Colors.grey[300]),
          const SizedBox(height: 16),
          Text(
            '暂无好友申请',
            style: TextStyle(color: Colors.grey[600], fontSize: 16),
          ),
        ],
      ),
    );
  }

  Widget _buildApplyItem(dynamic apply) {
    final int status = apply['status'] ?? 0; // 0:待处理, 1:已同意, 2:已拒绝
    final String fromNickname = apply['fromNickname'] ?? '未知用户';
    final String fromAvatar = apply['fromAvatar'] ?? '';
    final String remark = apply['remark'] ?? '请求添加你为好友';

    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      child: Row(
        children: [
          AvatarWidget(
            imageUrl: fromAvatar,
            name: fromNickname,
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  fromNickname,
                  style: const TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                Text(
                  remark,
                  style: TextStyle(
                    fontSize: 14,
                    color: Colors.grey[600],
                  ),
                ),
              ],
            ),
          ),
          if (status == 0)
            ElevatedButton(
              onPressed: () => _handleApply(apply['id'], 1),
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.green[600],
                foregroundColor: Colors.white,
                padding: const EdgeInsets.symmetric(horizontal: 16),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(4),
                ),
                elevation: 0,
              ),
              child: const Text('同意'),
            )
          else
            Text(
              status == 1 ? '已同意' : '已拒绝',
              style: TextStyle(color: Colors.grey[500]),
            ),
        ],
      ),
    );
  }
}
