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
          gradient: RadialGradient(
            center: Alignment(0, -0.6),
            radius: 1.1,
            colors: [Color(0xFF1B1436), Color(0xFF0D1117)],
          ),
        ),
        child: SafeArea(
          child: Center(
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 420),
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 32),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    // Logo placeholder.
                    Container(
                      width: 84,
                      height: 84,
                      decoration: BoxDecoration(
                        color: const Color(0xFF7C3AED),
                        borderRadius: BorderRadius.circular(22),
                        boxShadow: [
                          BoxShadow(
                            color: const Color(
                              0xFF7C3AED,
                            ).withValues(alpha: 0.35),
                            blurRadius: 32,
                            spreadRadius: 2,
                          ),
                        ],
                      ),
                      child: const Icon(
                        Icons.hub_outlined,
                        color: Colors.white,
                        size: 44,
                      ),
                    ),
                    const SizedBox(height: 28),
                    const Text(
                      'Rippo',
                      style: TextStyle(
                        color: Color(0xFFE6EDF3),
                        fontSize: 34,
                        fontWeight: FontWeight.bold,
                        letterSpacing: 0.5,
                      ),
                    ),
                    const SizedBox(height: 10),
                    const Text(
                      'Understand repositories with AI.',
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        color: Color(0xFF8B949E),
                        fontSize: 15,
                        height: 1.4,
                      ),
                    ),
                    const SizedBox(height: 44),
                    SizedBox(
                      width: double.infinity,
                      child: FilledButton.icon(
                        onPressed: () => _startOAuthLogin(context),
                        icon: const Icon(Icons.code, size: 20),
                        label: const Text('Continue with GitHub'),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
