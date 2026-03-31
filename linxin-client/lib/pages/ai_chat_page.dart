import 'package:flutter/material.dart';
import '../services/ai_service.dart';
import '../services/ai_intent_service.dart';

class AIChatPage extends StatefulWidget {
  const AIChatPage({super.key});

  @override
  State<AIChatPage> createState() => _AIChatPageState();
}

class _AIChatPageState extends State<AIChatPage> {
  final TextEditingController _inputController = TextEditingController();
  final List<AIChatMessage> _messages = [];
  bool _isLoading = false;
  AIChatResponse? _pendingResponse;
  bool _showConfirmDialog = false;
  String? _loadingMessageId;

  @override
  void dispose() {
    _inputController.dispose();
    super.dispose();
  }

  Future<void> _sendMessage() async {
    final text = _inputController.text.trim();
    if (text.isEmpty) return;

    _inputController.clear();

    setState(() {
      _messages.add(AIChatMessage(
        content: text,
        isUser: true,
      ));
      _isLoading = true;
    });

    try {
      final response = await AIService.instance.chat(
        AIChatRequest(userInput: text),
      );

      setState(() {
        _isLoading = false;
        if (response.toolCalls.isNotEmpty) {
          _pendingResponse = response;
          _showConfirmDialog = true;
          _messages.add(AIChatMessage(
            content: response.aiText,
            isUser: false,
            response: response,
          ));
        } else {
          _messages.add(AIChatMessage(
            content: response.aiText,
            isUser: false,
          ));
        }
      });
    } catch (e) {
      setState(() {
        _isLoading = false;
        _messages.add(AIChatMessage(
          content: '出错了: $e',
          isUser: false,
          isError: true,
        ));
      });
    }
  }

  Future<void> _executePendingActions() async {
    if (_pendingResponse == null) return;

    setState(() {
      _showConfirmDialog = false;
    });

    for (final toolCall in _pendingResponse!.toolCalls) {
      setState(() {
        _messages.add(AIChatMessage(
          content: '正在执行: ${toolCall.description}...',
          isUser: false,
          isLoading: true,
        ));
        _loadingMessageId = '${_messages.length - 1}';
      });

      final result = await AIIntentService.instance.executeToolCall(toolCall);

      setState(() {
        if (_loadingMessageId != null) {
          final index = int.tryParse(_loadingMessageId!);
          if (index != null && index < _messages.length) {
            _messages[index] = AIChatMessage(
              content: result.message,
              isUser: false,
              isError: !result.success,
            );
          }
        }
        _loadingMessageId = null;
      });
    }

    setState(() {
      _pendingResponse = null;
    });
  }

  Future<void> _modifyLastAction() async {
    final modifyText = await _showModifyDialog();
    if (modifyText == null || modifyText.isEmpty) return;

    setState(() {
      _showConfirmDialog = false;
      _isLoading = true;
    });

    try {
      final response = await AIService.instance.modifyParams(
        ModifyParamsRequest(
          modification: modifyText,
          originalResponse: _pendingResponse!,
        ),
      );

      setState(() {
        _isLoading = false;
        _pendingResponse = response;
        _showConfirmDialog = response.toolCalls.isNotEmpty;
        _messages.add(AIChatMessage(
          content: response.aiText,
          isUser: false,
          response: response,
        ));
      });
    } catch (e) {
      setState(() {
        _isLoading = false;
        _messages.add(AIChatMessage(
          content: '修改失败: $e',
          isUser: false,
          isError: true,
        ));
      });
    }
  }

  Future<String?> _showModifyDialog() async {
    final controller = TextEditingController();
    return showDialog<String>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('修改操作'),
        content: TextField(
          controller: controller,
          decoration: const InputDecoration(
            hintText: '请输入您的修改意见...',
            border: OutlineInputBorder(),
          ),
          maxLines: 3,
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('取消'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, controller.text),
            child: const Text('确认修改'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('AI 助手'),
      ),
      body: Column(
        children: [
          Expanded(
            child: ListView.builder(
              padding: const EdgeInsets.all(16),
              itemCount: _messages.length,
              itemBuilder: (context, index) {
                final message = _messages[index];
                return _buildMessageBubble(message);
              },
            ),
          ),
          if (_isLoading)
            const Padding(
              padding: EdgeInsets.all(8),
              child: CircularProgressIndicator(),
            ),
          if (_showConfirmDialog && _pendingResponse != null)
            _buildConfirmBar(_pendingResponse!),
          _buildInputBar(),
        ],
      ),
    );
  }

  Widget _buildMessageBubble(AIChatMessage message) {
    return Align(
      alignment: message.isUser ? Alignment.centerRight : Alignment.centerLeft,
      child: Container(
        margin: const EdgeInsets.symmetric(vertical: 4),
        padding: const EdgeInsets.all(12),
        constraints: BoxConstraints(
          maxWidth: MediaQuery.of(context).size.width * 0.75,
        ),
        decoration: BoxDecoration(
          color: message.isUser
              ? Theme.of(context).primaryColor
              : message.isError
                  ? Colors.red.shade100
                  : Colors.grey.shade200,
          borderRadius: BorderRadius.circular(16),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (message.isLoading)
              const Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  SizedBox(
                    width: 16,
                    height: 16,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  ),
                  SizedBox(width: 8),
                  Text('执行中...'),
                ],
              )
            else ...[
              Text(
                message.content,
                style: TextStyle(
                  color: message.isUser ? Colors.white : Colors.black,
                ),
              ),
              if (message.response?.toolCalls.isNotEmpty == true) ...[
                const SizedBox(height: 8),
                const Divider(),
                const SizedBox(height: 4),
                Text(
                  '将执行 ${message.response!.toolCalls.length} 个操作',
                  style: TextStyle(
                    fontSize: 12,
                    color: message.isUser ? Colors.white70 : Colors.grey,
                  ),
                ),
              ],
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildConfirmBar(AIChatResponse response) {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: Colors.blue.shade50,
        border: Border(top: BorderSide(color: Colors.blue.shade200)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(
            '即将执行 ${response.toolCalls.length} 个操作：',
            style: const TextStyle(fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 8),
          ...response.toolCalls.asMap().entries.map((entry) {
            final index = entry.key;
            final call = entry.value;
            return Padding(
              padding: const EdgeInsets.symmetric(vertical: 4),
              child: Row(
                children: [
                  CircleAvatar(
                    radius: 12,
                    child: Text('${index + 1}'),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      call.description,
                      style: const TextStyle(fontSize: 14),
                    ),
                  ),
                ],
              ),
            );
          }),
          const SizedBox(height: 12),
          Row(
            mainAxisAlignment: MainAxisAlignment.end,
            children: [
              TextButton(
                onPressed: () {
                  setState(() {
                    _showConfirmDialog = false;
                    _pendingResponse = null;
                  });
                },
                child: const Text('取消'),
              ),
              const SizedBox(width: 8),
              TextButton(
                onPressed: _modifyLastAction,
                child: const Text('修改'),
              ),
              const SizedBox(width: 8),
              ElevatedButton(
                onPressed: _executePendingActions,
                child: const Text('确认执行'),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildInputBar() {
    return Container(
      padding: const EdgeInsets.all(8),
      decoration: BoxDecoration(
        color: Colors.white,
        boxShadow: [
          BoxShadow(
            color: Colors.grey.withValues(alpha: 0.2),
            blurRadius: 4,
            offset: const Offset(0, -2),
          ),
        ],
      ),
      child: SafeArea(
        child: Row(
          children: [
            Expanded(
              child: TextField(
                controller: _inputController,
                decoration: InputDecoration(
                  hintText: '输入您想做的事...',
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(24),
                  ),
                  contentPadding: const EdgeInsets.symmetric(
                    horizontal: 16,
                    vertical: 8,
                  ),
                ),
                onSubmitted: (_) => _sendMessage(),
              ),
            ),
            const SizedBox(width: 8),
            IconButton(
              icon: const Icon(Icons.send),
              onPressed: _isLoading ? null : _sendMessage,
            ),
          ],
        ),
      ),
    );
  }
}

class AIChatMessage {
  final String content;
  final bool isUser;
  final bool isError;
  final bool isLoading;
  final AIChatResponse? response;

  AIChatMessage({
    required this.content,
    required this.isUser,
    this.isError = false,
    this.isLoading = false,
    this.response,
  });
}