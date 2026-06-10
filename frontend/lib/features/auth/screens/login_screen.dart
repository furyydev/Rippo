import 'package:flutter/material.dart';
import 'package:reppo/core/external_browser.dart';
import 'package:reppo/screens/home_screen.dart';
import 'package:reppo/services/api_service.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  @override
  void initState() {
    super.initState();
    ExternalBrowser.setAuthCallback(_handleAuthCallback);
    _checkInitialAuthCallback();
  }

  @override
  void dispose() {
    ExternalBrowser.setAuthCallback(null);
    super.dispose();
  }

  Future<void> _checkInitialAuthCallback() async {
    final authUri = await ExternalBrowser.getInitialAuthUri();

    if (authUri != null) {
      _handleAuthCallback(authUri);
    }
  }

  Future<void> _startOAuthLogin(BuildContext context) async {
    try {
      await ExternalBrowser.launch(ExternalBrowser.oauthLoginUrl);
    } catch (e) {
      if (!context.mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('Could not open login page: $e')));
    }
  }

  void _handleAuthCallback(String authUri) {
    final uri = Uri.parse(authUri);
    final sessionId = uri.queryParameters['sessionId'];

    if (sessionId == null || sessionId.isEmpty) {
      return;
    }

    ApiService.sessionId = sessionId;

    if (!mounted) return;

    Navigator.pushReplacement(
      context,
      MaterialPageRoute(
        builder: (context) {
          return const HomeScreen();
        },
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Container(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            colors: [Color(0xFF6A11CB), Color(0xFF2575FC)],
          ),
        ),
        child: Center(
          child: Material(
            color: Colors.transparent,
            child: InkWell(
              onTap: () => _startOAuthLogin(context),
              borderRadius: BorderRadius.circular(12),
              child: Ink(
                decoration: BoxDecoration(
                  color: Colors.white.withOpacity(0.2),
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: Colors.white, width: 1.5),
                ),
                child: const Padding(
                  padding: EdgeInsets.symmetric(
                    horizontal: 32,
                    vertical: 14,
                  ),
                  child: Text(
                    'Login',
                    style: TextStyle(
                      color: Colors.white,
                      fontSize: 24,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
