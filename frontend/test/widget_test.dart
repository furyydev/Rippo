import 'package:flutter_test/flutter_test.dart';
import 'package:reppo/main.dart';

void main() {
  testWidgets('shows the GitHub login entry screen', (
    WidgetTester tester,
  ) async {
    await tester.pumpWidget(const MyApp());

    expect(find.text('Login'), findsOneWidget);
  });
}
