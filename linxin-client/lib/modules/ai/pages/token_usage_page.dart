import 'package:flutter/material.dart';
import 'package:lin_xin/core/service/http_service.dart';

class TokenUsagePage extends StatefulWidget {
  const TokenUsagePage({super.key});

  @override
  State<TokenUsagePage> createState() => _TokenUsagePageState();
}

class _TokenUsagePageState extends State<TokenUsagePage> {
  bool _isLoading = true;
  int _selectedDays = 3;
  Map<String, dynamic> _stats = {'daily': [], 'intents': []};

  @override
  void initState() {
    super.initState();
    _loadUsage();
  }

  Future<void> _loadUsage() async {
    setState(() => _isLoading = true);
    try {
      final response = await HttpService().get('/api/ai/usage', queryParameters: {'days': _selectedDays});
      if (response.data != null) {
        setState(() {
          _stats = Map<String, dynamic>.from(response.data);
          _isLoading = false;
        });
      }
    } catch (e) {
      setState(() => _isLoading = false);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('加载失败: $e')));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.grey[50],
      appBar: AppBar(
        title: const Text('Token 使用统计'),
        backgroundColor: Colors.white,
        foregroundColor: Colors.black,
        elevation: 0.5,
        actions: [
          PopupMenuButton<int>(
            icon: const Icon(Icons.filter_list),
            onSelected: (days) {
              setState(() {
                _selectedDays = days;
              });
              _loadUsage();
            },
            itemBuilder: (context) => [
              const PopupMenuItem(value: 3, child: Text('最近 3 天')),
              const PopupMenuItem(value: 7, child: Text('最近 7 天')),
              const PopupMenuItem(value: 30, child: Text('最近 30 天')),
              const PopupMenuItem(value: 90, child: Text('最近 90 天')),
            ],
          ),
        ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : RefreshIndicator(
              onRefresh: _loadUsage,
              child: ListView(
                padding: const EdgeInsets.all(16),
                children: [
                  _buildSectionTitle('每日使用情况'),
                  _buildDailyUsageList(),
                  const SizedBox(height: 24),
                  _buildSectionTitle('工具/接口使用情况'),
                  _buildIntentUsageList(),
                ],
              ),
            ),
    );
  }

  Widget _buildSectionTitle(String title) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12, left: 4),
      child: Text(
        title,
        style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
      ),
    );
  }

  Widget _buildDailyUsageList() {
    final daily = _stats['daily'] as List<dynamic>? ?? [];
    if (daily.isEmpty) return _buildEmptyCard('暂无数据');

    return Card(
      elevation: 0,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: Column(
        children: daily.map((item) {
          return ListTile(
            title: Text(item['date']?.toString() ?? ''),
            trailing: Text(
              '${item['totalTokens']} tokens',
              style: const TextStyle(fontWeight: FontWeight.bold, color: Colors.green),
            ),
          );
        }).toList(),
      ),
    );
  }

  Widget _buildIntentUsageList() {
    final intents = _stats['intents'] as List<dynamic>? ?? [];
    if (intents.isEmpty) return _buildEmptyCard('暂无数据');

    return Card(
      elevation: 0,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: Column(
        children: intents.map((item) {
          return ListTile(
            leading: const Icon(Icons.extension_outlined, color: Colors.blue),
            title: Text(item['intent']?.toString() ?? '未知工具'),
            trailing: Text(
              '${item['totalTokens']} tokens',
              style: const TextStyle(fontWeight: FontWeight.bold),
            ),
          );
        }).toList(),
      ),
    );
  }

  Widget _buildEmptyCard(String message) {
    return Card(
      elevation: 0,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Center(
          child: Text(message, style: TextStyle(color: Colors.grey[400])),
        ),
      ),
    );
  }
}
