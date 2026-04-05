import 'package:flutter/material.dart';

class GeneralSettingsPage extends StatelessWidget {
  const GeneralSettingsPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('通用设置')),
      body: ListView(
        children: [
          const SizedBox(height: 12),
          ListTile(
            leading: const Icon(Icons.notifications_none),
            title: const Text('新消息通知'),
            trailing: Switch(value: true, onChanged: (v) {}),
          ),
          const Divider(height: 1),
          ListTile(
            leading: const Icon(Icons.cleaning_services_outlined),
            title: const Text('清除缓存'),
            onTap: () {
              showDialog(
                context: context,
                builder: (context) => AlertDialog(
                  title: const Text('提示'),
                  content: const Text('确定要清除所有本地缓存吗？'),
                  actions: [
                    TextButton(onPressed: () => Navigator.pop(context), child: const Text('取消')),
                    TextButton(
                      onPressed: () {
                        Navigator.pop(context);
                        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('缓存已清除')));
                      }, 
                      child: const Text('确定'),
                    ),
                  ],
                ),
              );
            },
          ),
          const Divider(height: 1),
          ListTile(
            leading: const Icon(Icons.info_outline),
            title: const Text('关于灵信'),
            trailing: const Text('v1.0.0', style: TextStyle(color: Colors.grey)),
            onTap: () {
              showAboutDialog(
                context: context,
                applicationName: '灵信',
                applicationVersion: '1.0.0',
                applicationIcon: const FlutterLogo(size: 50),
                children: [
                  const Text('灵信是一款追求极致体验的即时通讯应用。'),
                ],
              );
            },
          ),
          const Divider(height: 1),
        ],
      ),
    );
  }
}
