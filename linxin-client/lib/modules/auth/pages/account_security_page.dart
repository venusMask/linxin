import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:lin_xin/modules/auth/auth_service.dart';

class AccountSecurityPage extends StatelessWidget {
  const AccountSecurityPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('账号安全')),
      body: ListView(
        children: [
          const SizedBox(height: 12),
          ListTile(
            title: const Text('修改登录密码'),
            trailing: const Icon(Icons.chevron_right),
            onTap: () {
              showDialog(context: context, builder: (context) => _EditPasswordDialog());
            },
          ),
          const Divider(height: 1),
          ListTile(
            title: const Text('登录历史'),
            trailing: const Icon(Icons.chevron_right),
            onTap: () {
              ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('功能开发中...')));
            },
          ),
          const Divider(height: 1),
        ],
      ),
    );
  }
}

class _EditPasswordDialog extends StatefulWidget {
  @override
  State<_EditPasswordDialog> createState() => _EditPasswordDialogState();
}

class _EditPasswordDialogState extends State<_EditPasswordDialog> {
  final _oldPwdController = TextEditingController();
  final _newPwdController = TextEditingController();
  final _confirmPwdController = TextEditingController();
  bool _isLoading = false;

  @override
  void dispose() {
    _oldPwdController.dispose();
    _newPwdController.dispose();
    _confirmPwdController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text('修改登录密码'),
      content: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: _oldPwdController,
              obscureText: true,
              decoration: const InputDecoration(labelText: '原密码', hintText: '请输入旧密码'),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _newPwdController,
              obscureText: true,
              decoration: const InputDecoration(labelText: '新密码', hintText: '至少6位'),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _confirmPwdController,
              obscureText: true,
              decoration: const InputDecoration(labelText: '确认新密码'),
            ),
          ],
        ),
      ),
      actions: [
        TextButton(onPressed: () => Navigator.pop(context), child: const Text('取消')),
        ElevatedButton(
          onPressed: _isLoading ? null : _handleUpdate,
          child: _isLoading ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2)) : const Text('确定'),
        ),
      ],
    );
  }

  Future<void> _handleUpdate() async {
    final oldP = _oldPwdController.text;
    final newP = _newPwdController.text;
    final confP = _confirmPwdController.text;

    if (oldP.isEmpty || newP.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('请填写完整信息')));
      return;
    }
    if (newP.length < 6) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('新密码至少需要6位')));
      return;
    }
    if (newP != confP) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('两次输入的新密码不一致')));
      return;
    }

    setState(() => _isLoading = true);
    try {
      await context.read<AuthService>().updatePassword(oldPassword: oldP, newPassword: newP);
      if (mounted) {
        Navigator.pop(context);
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('密码修改成功')));
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('修改失败: $e')));
      }
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }
}
