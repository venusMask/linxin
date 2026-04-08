import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:lin_xin/core/pages/main_page.dart';
import 'package:lin_xin/modules/auth/pages/login_page.dart';
import 'package:lin_xin/modules/auth/auth_service.dart';
import 'package:lin_xin/core/state/data_service.dart';
import 'package:lin_xin/core/service/log_service.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await LogService().init();
  final authService = AuthService();
  await authService.initialize();

  final dataService = DataService();
  await dataService.initialize();
  
  runApp(
    MultiProvider(
      providers: [
        ChangeNotifierProvider.value(value: authService),
        ChangeNotifierProvider.value(value: dataService),
      ],
      child: const MyApp(),
    ),
  );
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: '灵信',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFF00BFA5), // 现代薄荷绿
          primary: const Color(0xFF00BFA5),
          surface: const Color(0xFFF7F7F7),
        ),
        useMaterial3: true,
        scaffoldBackgroundColor: const Color(0xFFF7F7F7),
        appBarTheme: const AppBarTheme(
          backgroundColor: Colors.white,
          elevation: 0,
          centerTitle: true,
          titleTextStyle: TextStyle(
            color: Color(0xFF1A1A1A),
            fontSize: 18,
            fontWeight: FontWeight.w600,
          ),
          iconTheme: IconThemeData(color: Color(0xFF1A1A1A)),
        ),
      ),
      home: const AuthWrapper(),
      debugShowCheckedModeBanner: false,
    );
  }
}

class AuthWrapper extends StatelessWidget {
  const AuthWrapper({super.key});

  @override
  Widget build(BuildContext context) {
    final authService = context.watch<AuthService>();

    if (!authService.isInitialized) {
      return const Scaffold(
        body: Center(
          child: CircularProgressIndicator(),
        ),
      );
    }

    if (authService.isLoggedIn) {
      return const MainPage();
    } else {
      return const LoginPage();
    }
  }
}
