import 'package:flutter/material.dart';
import '../services/auth_service.dart';
import '../widgets/avatar_widget.dart';
import 'agent_token_page.dart';
import 'edit_profile_page.dart';
import 'account_security_page.dart';
import 'general_settings_page.dart';

class ProfilePage extends StatelessWidget {
  const ProfilePage({super.key});

  Future<void> _logout(BuildContext context) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('确认登出'),
        content: const Text('确定要退出登录吗？'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('取消'),
          ),
          TextButton(
            onPressed: () => Navigator.of(context).pop(true),
            child: const Text('确定'),
          ),
        ],
      ),
    );

    if (confirmed == true) {
      AuthService().logout();
    }
  }

  @override
  Widget build(BuildContext context) {
    final user = AuthService().currentUser;

    return Scaffold(
      backgroundColor: Colors.grey[50],
      body: CustomScrollView(
        slivers: [
          SliverAppBar(
            expandedHeight: 200,
            pinned: true,
            flexibleSpace: FlexibleSpaceBar(
              background: Container(
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    begin: Alignment.topCenter,
                    end: Alignment.bottomCenter,
                    colors: [
                      Colors.green[600]!,
                      Colors.green[400]!,
                    ],
                  ),
                ),
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    const SizedBox(height: 40),
                    AvatarWidget(
                      imageUrl: null, // 或从用户对象获取
                      name: user?.nickname ?? user?.username ?? '用户',
                      size: 80,
                    ),
                    const SizedBox(height: 12),
                    Text(
                      user?.nickname ?? user?.username ?? '未知用户',
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 20,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    Text(
                      '灵信号: ${user?.username ?? ""}',
                      style: TextStyle(
                        color: Colors.white.withValues(alpha: 0.8),
                        fontSize: 14,
                      ),
                    ),
                    if (user?.signature != null && user!.signature!.isNotEmpty)
                      Padding(
                        padding: const EdgeInsets.only(top: 8, left: 32, right: 32),
                        child: Text(
                          user.signature!,
                          style: TextStyle(
                            color: Colors.white.withValues(alpha: 0.7),
                            fontSize: 12,
                            fontStyle: FontStyle.italic,
                          ),
                          textAlign: TextAlign.center,
                          maxLines: 2,
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                  ],
                ),
              ),
            ),
          ),
          SliverList(
            delegate: SliverChildListDelegate([
              const SizedBox(height: 12),
              _buildMenuItem(Icons.person_outline, '个人信息', () {
                Navigator.push(
                  context,
                  MaterialPageRoute(builder: (context) => const EditProfilePage()),
                );
              }),
              _buildMenuItem(Icons.smart_toy_outlined, 'Agent 开放平台', () {
                Navigator.push(
                  context,
                  MaterialPageRoute(builder: (context) => const AgentTokenPage()),
                );
              }),
              _buildMenuItem(Icons.security, '账号安全', () {
                Navigator.push(
                  context,
                  MaterialPageRoute(builder: (context) => const AccountSecurityPage()),
                );
              }),
              _buildMenuItem(Icons.settings_outlined, '通用设置', () {
                Navigator.push(
                  context,
                  MaterialPageRoute(builder: (context) => const GeneralSettingsPage()),
                );
              }),
              const SizedBox(height: 24),
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 16),
                child: ElevatedButton(
                  onPressed: () => _logout(context),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Colors.white,
                    foregroundColor: Colors.red,
                    padding: const EdgeInsets.symmetric(vertical: 16),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(12),
                    ),
                    elevation: 0,
                    side: BorderSide(color: Colors.red[100]!),
                  ),
                  child: const Text(
                    '退出登录',
                    style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                  ),
                ),
              ),
            ]),
          ),
        ],
      ),
    );
  }

  Widget _buildMenuItem(IconData icon, String title, VoidCallback onTap) {
    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),
      ),
      child: ListTile(
        leading: Icon(icon, color: Colors.green[600]),
        title: Text(title),
        trailing: const Icon(Icons.chevron_right, size: 20),
        onTap: onTap,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      ),
    );
  }
}
