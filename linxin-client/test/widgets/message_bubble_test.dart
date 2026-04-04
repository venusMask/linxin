import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:lin_xin/models/message.dart';
import 'package:lin_xin/widgets/message_bubble.dart';

void main() {
  group('MessageBubble Widget Tests', () {
    testWidgets('should show agent attribution when isAi is true', (WidgetTester tester) async {
      final message = Message(
        id: '1',
        conversationId: 'c1',
        senderId: 's1',
        content: 'AI Content',
        isAi: true,
        senderType: 'OpenClaw',
        createdAt: DateTime.now(),
      );

      await tester.pumpWidget(MaterialApp(
        home: Scaffold(
          body: MessageBubble(message: message),
        ),
      ));

      expect(find.text('AI Content'), findsOneWidget);
      expect(find.text('来自 OpenClaw 辅助'), findsOneWidget);
      expect(find.byIcon(Icons.smart_toy_outlined), findsOneWidget);
    });

    testWidgets('should NOT show agent attribution for normal message', (WidgetTester tester) async {
      final message = Message(
        id: '2',
        conversationId: 'c1',
        senderId: 's1',
        content: 'Normal Content',
        isAi: false,
        createdAt: DateTime.now(),
      );

      await tester.pumpWidget(MaterialApp(
        home: Scaffold(
          body: MessageBubble(message: message),
        ),
      ));

      expect(find.text('Normal Content'), findsOneWidget);
      expect(find.textContaining('辅助'), findsNothing);
    });
  });
}
