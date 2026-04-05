import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'dart:async';
import 'package:lin_xin/modules/auth/auth_service.dart';
import 'package:lin_xin/widgets/avatar_widget.dart';

class EditProfilePage extends StatefulWidget {
  const EditProfilePage({super.key});

  @override
  State<EditProfilePage> createState() => _EditProfilePageState();
}

class _EditProfilePageState extends State<EditProfilePage> {
  final _formKey = GlobalKey<FormState>();
  late TextEditingController _nicknameController;
  late TextEditingController _usernameController;
  late TextEditingController _signatureController;
  int? _gender;
  bool _isSaving = false;

  @override
  void initState() {
    super.initState();
    final user = context.read<AuthService>().currentUser;
    _nicknameController = TextEditingController(text: user?.nickname);
    _usernameController = TextEditingController(text: user?.username);
    _signatureController = TextEditingController(text: user?.signature);
    _gender = user?.gender;
  }

  @override
  void dispose() {
    _nicknameController.dispose();
    _usernameController.dispose();
    _signatureController.dispose();
    super.dispose();
  }

  Future<void> _saveProfile() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() => _isSaving = true);
    try {
      await context.read<AuthService>().updateProfile(
        nickname: _nicknameController.text.trim(),
        username: _usernameController.text.trim(),
        signature: _signatureController.text.trim(),
        gender: _gender,
      );
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('保存成功')));
        Navigator.pop(context);
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('保存失败: $e')));
      }
    } finally {
      if (mounted) setState(() => _isSaving = false);
    }
  }

  Future<void> _changeEmail() async {
    final passwordController = TextEditingController();
    final emailController = TextEditingController();
    final codeController = TextEditingController();
    bool isSendingCode = false;
    int countdown = 0;
    Timer? timer;

    await showDialog(
      context: context,
      builder: (context) => StatefulBuilder(
        builder: (context, setDialogState) => AlertDialog(
          title: const Text('修改邮箱'),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextField(
                  controller: passwordController,
                  obscureText: true,
                  decoration: const InputDecoration(labelText: '当前密码', hintText: '验证身份'),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: emailController,
                  decoration: const InputDecoration(labelText: '新邮箱', hintText: '请输入新邮箱地址'),
                ),
                const SizedBox(height: 12),
                Row(
                  children: [
                    Expanded(
                      child: TextField(
                        controller: codeController,
                        decoration: const InputDecoration(labelText: '验证码'),
                      ),
                    ),
                    TextButton(
                      onPressed: (countdown > 0 || isSendingCode) 
                        ? null 
                        : () async {
                            if (emailController.text.isEmpty) return;
                            setDialogState(() => isSendingCode = true);
                            try {
                              await context.read<AuthService>().sendEmailVerificationCode(
                                emailController.text.trim(),
                                type: 'change_email',
                              );
                              setDialogState(() {
                                isSendingCode = false;
                                countdown = 60;
                              });
                              timer = Timer.periodic(const Duration(seconds: 1), (t) {
                                if (countdown > 0) {
                                  setDialogState(() => countdown--);
                                } else {
                                  t.cancel();
                                }
                              });
                            } catch (e) {
                              setDialogState(() => isSendingCode = false);
                              if (context.mounted) {
                                ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('发送失败: $e')));
                              }
                            }
                          },
                      child: Text(countdown > 0 ? '$countdown秒后重发' : '获取验证码'),
                    ),
                  ],
                ),
              ],
            ),
          ),
          actions: [
            TextButton(onPressed: () {
              timer?.cancel();
              Navigator.pop(context);
            }, child: const Text('取消')),
            ElevatedButton(
              onPressed: () async {
                try {
                  await context.read<AuthService>().updateEmail(
                    password: passwordController.text,
                    newEmail: emailController.text.trim(),
                    code: codeController.text.trim(),
                  );
                  timer?.cancel();
                  if (context.mounted) {
                    Navigator.pop(context);
                    ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('邮箱修改成功')));
                  }
                } catch (e) {
                  if (context.mounted) {
                    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('修改失败: $e')));
                  }
                }
              },
              child: const Text('确认修改'),
            ),
          ],
        ),
      ),
    );
    timer?.cancel();
  }

  @override
  Widget build(BuildContext context) {
    final user = context.watch<AuthService>().currentUser;

    return Scaffold(
      appBar: AppBar(
        title: const Text('编辑资料'),
        actions: [
          if (_isSaving)
            const Padding(
              padding: EdgeInsets.symmetric(horizontal: 16),
              child: Center(child: SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2))),
            )
          else
            TextButton(
              onPressed: _saveProfile,
              child: const Text('保存', style: TextStyle(fontWeight: FontWeight.bold)),
            ),
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              const SizedBox(height: 20),
              Stack(
                children: [
                  AvatarWidget(
                    imageUrl: user?.avatar,
                    name: user?.nickname ?? user?.username ?? '',
                    size: 100,
                  ),
                  PositionBag(
                    bottom: 0,
                    right: 0,
                    child: Container(
                      padding: const EdgeInsets.all(4),
                      decoration: const BoxDecoration(color: Colors.green, shape: BoxShape.circle),
                      child: const Icon(Icons.camera_alt, color: Colors.white, size: 20),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 32),
              _buildTextField('昵称', _nicknameController, Icons.person_outline),
              const SizedBox(height: 16),
              _buildTextField('用户名', _usernameController, Icons.alternate_email),
              const SizedBox(height: 16),
              _buildTextField('个性签名', _signatureController, Icons.edit_note, maxLines: 3),
              const SizedBox(height: 16),
              _buildGenderPicker(),
              const SizedBox(height: 24),
              const Divider(),
              ListTile(
                leading: const Icon(Icons.email_outlined),
                title: const Text('绑定邮箱'),
                subtitle: Text(user?.email ?? '未绑定'),
                trailing: const Icon(Icons.chevron_right),
                onTap: _changeEmail,
              ),
              const Divider(),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildTextField(String label, TextEditingController controller, IconData icon, {int maxLines = 1}) {
    return TextFormField(
      controller: controller,
      maxLines: maxLines,
      decoration: InputDecoration(
        labelText: label,
        prefixIcon: Icon(icon),
        border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
      ),
      validator: (value) {
        if (value == null || value.isEmpty) return '请输入$label';
        return null;
      },
    );
  }

  Widget _buildGenderPicker() {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
      decoration: BoxDecoration(
        border: Border.all(color: Colors.grey),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Row(
        children: [
          const Icon(Icons.wc, color: Colors.grey),
          const SizedBox(width: 12),
          const Text('性别'),
          const Spacer(),
          DropdownButton<int>(
            value: _gender,
            underline: const SizedBox(),
            items: const [
              DropdownMenuItem(value: 0, child: Text('未知')),
              DropdownMenuItem(value: 1, child: Text('男')),
              DropdownMenuItem(value: 2, child: Text('女')),
            ],
            onChanged: (val) => setState(() => _gender = val),
          ),
        ],
      ),
    );
  }
}

class PositionBag extends StatelessWidget {
  final double? top, bottom, left, right;
  final Widget child;
  const PositionBag({super.key, this.top, this.bottom, this.left, this.right, required this.child});
  @override
  Widget build(BuildContext context) {
    return Positioned(top: top, bottom: bottom, left: left, right: right, child: child);
  }
}
