import 'package:flutter/material.dart';

class VUPainter extends CustomPainter {
  Color lineColor;
  double width;

  double level = 0;

  VUPainter({required this.lineColor, required this.level, this.width = 0});

  @override
  void paint(Canvas canvas, Size size) {

    Paint line = Paint()
      ..color = lineColor
      ..strokeCap = StrokeCap.round
      ..style = PaintingStyle.stroke
      ..strokeWidth = 8;

    double offsetX = (100 + level) * 2;

    canvas.drawLine(const Offset(0, 0), Offset(offsetX, 0), line);
  }

  @override
  bool shouldRepaint(CustomPainter oldDelegate) {
    return true;
  }
}
