import 'package:flutter/material.dart';
import 'pages/main_page.dart';
import 'pages/login_page.dart';
import 'services/auth_service.dart';

import 'services/log_service.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await LogService().init();
  await AuthService().initialize();
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: '灵信',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.green),
        useMaterial3: true,
      ),
      home: const AuthWrapper(),
      debugShowCheckedModeBanner: false,
    );
  }
}

class AuthWrapper extends StatefulWidget {
  const AuthWrapper({super.key});

  @override
  State<AuthWrapper> createState() => _AuthWrapperState();
}

class _AuthWrapperState extends State<AuthWrapper> {
  final AuthService _authService = AuthService();
  bool _isCheckingAuth = true;

  @override
  void initState() {
    super.initState();
    _authService.addListener(_handleAuthChange);
    _checkAuthStatus();
  }

  @override
  void dispose() {
    _authService.removeListener(_handleAuthChange);
    super.dispose();
  }

  void _handleAuthChange() {
    if (mounted) {
      setState(() {});
    }
  }

  Future<void> _checkAuthStatus() async {
    await Future.doWhile(() async {
      if (!mounted) return false;
      if (_authService.isInitialized) return false;
      await Future.delayed(const Duration(milliseconds: 50));
      return !_authService.isInitialized;
    });

    if (mounted) {
      setState(() {
        _isCheckingAuth = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_isCheckingAuth) {
      return const Scaffold(
        body: Center(
          child: CircularProgressIndicator(),
        ),
      );
    }

    if (_authService.isLoggedIn) {
      return const MainPage();
    } else {
      return const LoginPage();
    }
  }
}
