import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mockito/mockito.dart';
import 'package:dio/dio.dart';
import 'package:lin_xin/pages/token_usage_page.dart';
import 'package:lin_xin/services/http_service.dart';

class MockHttpService extends Mock implements HttpService {
  @override
  Future<Response> get(String? path, {Map<String, dynamic>? queryParameters}) =>
      super.noSuchMethod(
        Invocation.method(#get, [path], {#queryParameters: queryParameters}),
        returnValue: Future.value(Response(requestOptions: RequestOptions(path: path ?? ''))),
      );
}

void main() {
  late MockHttpService mockHttpService;

  setUp(() {
    mockHttpService = MockHttpService();
    HttpService.setMock(mockHttpService);
  });

  testWidgets('TokenUsagePage displays data correctly', (WidgetTester tester) async {
    // Prepare mock data
    final mockData = {
      'daily': [
        {'date': '2024-03-20', 'totalTokens': 1000},
        {'date': '2024-03-21', 'totalTokens': 1500},
      ],
      'intents': [
        {'intent': 'chat', 'totalTokens': 2000},
        {'intent': 'code', 'totalTokens': 500},
      ]
    };

    // Use specific path to avoid null-safety issues with 'any' on non-nullable positional params
    when(mockHttpService.get('/api/ai/usage', queryParameters: anyNamed('queryParameters')))
        .thenAnswer((_) async => Response(
              data: mockData,
              requestOptions: RequestOptions(path: '/api/ai/usage'),
            ));

    // Build the widget
    await tester.pumpWidget(const MaterialApp(home: TokenUsagePage()));

    // Wait for data to load
    await tester.pump();

    // Verify daily usage
    expect(find.text('2024-03-20'), findsOneWidget);
    expect(find.text('1000 tokens'), findsOneWidget);
    expect(find.text('2024-03-21'), findsOneWidget);
    expect(find.text('1500 tokens'), findsOneWidget);

    // Verify tool usage
    expect(find.text('chat'), findsOneWidget);
    expect(find.text('2000 tokens'), findsOneWidget);
    expect(find.text('code'), findsOneWidget);
    expect(find.text('500 tokens'), findsOneWidget);
  });

  testWidgets('TokenUsagePage shows empty state', (WidgetTester tester) async {
    when(mockHttpService.get('/api/ai/usage', queryParameters: anyNamed('queryParameters')))
        .thenAnswer((_) async => Response(
              data: {'daily': [], 'intents': []},
              requestOptions: RequestOptions(path: '/api/ai/usage'),
            ));

    await tester.pumpWidget(const MaterialApp(home: TokenUsagePage()));
    await tester.pump();

    expect(find.text('暂无数据'), findsNWidgets(2));
  });
}
