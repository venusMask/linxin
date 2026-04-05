import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:lin_xin/core/service/http_service.dart';

class AgentTokenPage extends StatefulWidget {
  const AgentTokenPage({super.key});

  @override
  State<AgentTokenPage> createState() => _AgentTokenPageState();
}

class _AgentTokenPageState extends State<AgentTokenPage> {
  List<dynamic> _tokens = [];
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadTokens();
  }

  Future<void> _loadTokens() async {
    setState(() => _isLoading = true);
    try {
      final response = await HttpService().get('/api/agent/tokens');
      setState(() {
        _tokens = response.data;
        _isLoading = false;
      });
    } catch (e) {
      setState(() => _isLoading = false);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('加载失败: $e')));
      }
    }
  }

  Future<void> _generateNewToken() async {
    final nameController = TextEditingController();
    int selectedDays = 30; // 默认30天
    List<String> selectedScopes = ['msg:send', 'contact:read', 'user:profile'];

    final result = await showDialog<Map<String, dynamic>>(
      context: context,
      builder: (context) => StatefulBuilder(
        builder: (context, setDialogState) => AlertDialog(
          title: const Text('生成新令牌'),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('Agent 名称', style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold)),
                const SizedBox(height: 8),
                TextField(
                  controller: nameController,
                  decoration: const InputDecoration(
                    hintText: '如: OpenClaw',
                    border: OutlineInputBorder(),
                    contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                  ),
                  autofocus: true,
                ),
                const SizedBox(height: 20),
                const Text('有效期', style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold)),
                const SizedBox(height: 8),
                DropdownButtonFormField<int>(
                  initialValue: selectedDays,
                  decoration: const InputDecoration(
                    border: OutlineInputBorder(),
                    contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                  ),
                  items: const [
                    DropdownMenuItem(value: 7, child: Text('7 天')),
                    DropdownMenuItem(value: 30, child: Text('30 天')),
                    DropdownMenuItem(value: 90, child: Text('90 天')),
                    DropdownMenuItem(value: 0, child: Text('永久有效')),
                  ],
                  onChanged: (val) {
                    if (val != null) setDialogState(() => selectedDays = val);
                  },
                ),
                const SizedBox(height: 20),
                const Text('权限范围 (Scopes)', style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold)),
                const SizedBox(height: 8),
                _buildScopeItem('msg:send', '发送消息', selectedScopes, setDialogState),
                _buildScopeItem('msg:read', '读取消息', selectedScopes, setDialogState),
                _buildScopeItem('contact:read', '关系链读取', selectedScopes, setDialogState),
                _buildScopeItem('user:profile', '个人资料', selectedScopes, setDialogState),
              ],
            ),
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(context), child: const Text('取消')),
            ElevatedButton(
              onPressed: () => Navigator.pop(context, {
                'name': nameController.text.trim(),
                'days': selectedDays,
                'scopes': selectedScopes.join(','),
              }),
              style: ElevatedButton.styleFrom(backgroundColor: Colors.green[600], foregroundColor: Colors.white),
              child: const Text('生成'),
            ),
          ],
        ),
      ),
    );

    if (result != null && result['name']!.isNotEmpty) {
      try {
        await HttpService().post('/api/agent/tokens/generate', data: {
          'agentName': result['name'],
          'expireDays': result['days'],
          'scopes': result['scopes'],
        });
        _loadTokens();
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('生成失败: $e')));
        }
      }
    }
  }

  Future<void> _revokeToken(dynamic tokenObj) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('撤销授权'),
        content: Text('确定要撤销对 "${tokenObj['agentName']}" 的授权吗？撤销后相关 Agent 将无法再访问。'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('取消')),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('确定撤销', style: TextStyle(color: Colors.red)),
          ),
        ],
      ),
    );

    if (confirmed == true) {
      try {
        await HttpService().delete('/api/agent/tokens/${tokenObj['id']}');
        _loadTokens();
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('撤销失败: $e')));
        }
      }
    }
  }

  void _copyToClipboard(String text) {
    Clipboard.setData(ClipboardData(text: text));
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('令牌已复制到剪贴板')),
    );
  }

  Widget _buildScopeItem(String value, String label, List<String> selected, StateSetter setDialogState) {
    final bool isChecked = selected.contains(value);
    return CheckboxListTile(
      value: isChecked,
      title: Text(label, style: const TextStyle(fontSize: 14)),
      controlAffinity: ListTileControlAffinity.leading,
      dense: true,
      contentPadding: EdgeInsets.zero,
      onChanged: (bool? val) {
        setDialogState(() {
          if (val == true) {
            selected.add(value);
          } else {
            selected.remove(value);
          }
        });
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.grey[50],
      appBar: AppBar(
        title: const Text('Agent 授权管理'),
        backgroundColor: Colors.white,
        foregroundColor: Colors.black,
        elevation: 0.5,
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _tokens.isEmpty
              ? _buildEmptyState()
              : ListView.builder(
                  padding: const EdgeInsets.all(16),
                  itemCount: _tokens.length,
                  itemBuilder: (context, index) => _buildTokenCard(_tokens[index]),
                ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _generateNewToken,
        backgroundColor: Colors.green[600],
        icon: const Icon(Icons.add),
        label: const Text('生成新令牌'),
      ),
    );
  }

  Widget _buildEmptyState() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.vpn_key_outlined, size: 64, color: Colors.grey[300]),
          const SizedBox(height: 16),
          Text('暂无授权的 Agent', style: TextStyle(color: Colors.grey[600])),
          const SizedBox(height: 8),
          const Text('您可以生成令牌供第三方 Agent 调用', style: TextStyle(color: Colors.grey, fontSize: 12)),
        ],
      ),
    );
  }

  Widget _buildTokenCard(dynamic tokenObj) {
    final bool isActive = tokenObj['status'] == 1;
    final String tokenText = tokenObj['token'];
    final String? expireTime = tokenObj['expireTime'];

    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),
        boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.02), blurRadius: 4, offset: const Offset(0, 2))],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                tokenObj['agentName'],
                style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                decoration: BoxDecoration(
                  color: isActive ? Colors.green[50] : Colors.grey[100],
                  borderRadius: BorderRadius.circular(4),
                ),
                child: Text(
                  isActive ? '已启用' : '已撤销',
                  style: TextStyle(color: isActive ? Colors.green[700] : Colors.grey, fontSize: 11),
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: Colors.grey[50],
              borderRadius: BorderRadius.circular(8),
              border: Border.all(color: Colors.grey[200]!),
            ),
            child: Row(
              children: [
                Expanded(
                  child: Text(
                    isActive ? tokenText : '••••••••••••••••',
                    style: const TextStyle(fontFamily: 'monospace', fontSize: 12),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
                if (isActive)
                  IconButton(
                    icon: const Icon(Icons.copy, size: 18),
                    onPressed: () => _copyToClipboard(tokenText),
                    padding: EdgeInsets.zero,
                    constraints: const BoxConstraints(),
                  ),
              ],
            ),
          ),
          const SizedBox(height: 12),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '创建时间: ${tokenObj['createTime']?.replaceAll('T', ' ').substring(0, 16) ?? ""}',
                    style: const TextStyle(fontSize: 11, color: Colors.grey),
                  ),
                  Text(
                    expireTime == null ? '永久有效' : '过期时间: ${expireTime.replaceAll('T', ' ').substring(0, 16)}',
                    style: TextStyle(
                      fontSize: 11, 
                      color: expireTime == null ? Colors.blue[400] : Colors.orange[400],
                    ),
                  ),
                ],
              ),
              if (isActive)
                TextButton(
                  onPressed: () => _revokeToken(tokenObj),
                  child: const Text('撤销授权', style: TextStyle(color: Colors.red, fontSize: 13)),
                ),
            ],
          ),
        ],
      ),
    );
  }
}
