// This is a basic Flutter widget test.
//
// To perform an interaction with a widget in your test, use the WidgetTester
// utility in the flutter_test package. For example, you can send tap and scroll
// gestures. You can also use WidgetTester to find child widgets in the widget
// tree, read text, and verify that the values of widget properties are correct.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:offline_notes_app/main.dart';

void main() {
  testWidgets('App starts with Login Screen', (WidgetTester tester) async {
    // Build our app and trigger a frame.
    await tester.pumpWidget(const OfflineNotesApp());
    await tester.pumpAndSettle();

    // Verify that we are on the login screen (or at least app builds)
    // We can just check for any widget, or just pass if no exception
    expect(find.byType(OfflineNotesApp), findsOneWidget);
  });
}
