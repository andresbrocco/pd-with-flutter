import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  static const methodChannel = 'com.domain_name.app_name';
  static const platformChannel = MethodChannel(methodChannel);

  bool _playStatus = false;

  void _toggleAudio(bool newValue){

    var methodName = newValue ? 'startTone' : 'stopTone';

    platformChannel.invokeMethod(methodName);

    setState((){
      _playStatus = newValue;
    });
  }

  @override
  void initState() {
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      theme: ThemeData(
        primarySwatch: Colors.cyan,
      ),
      home: Scaffold(
      appBar: AppBar(
        title: const Text('Flutter, pd-for-android wrap demo'),
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            Text('sound: $_playStatus'),
            Switch(
              value: _playStatus,
              onChanged: _toggleAudio,
            ),
          ],
        ),
      ),
    ),
    );
  }
}
