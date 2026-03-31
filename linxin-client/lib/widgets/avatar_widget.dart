import 'package:flutter/material.dart';

class AvatarWidget extends StatelessWidget {
  final String? imageUrl;
  final double size;
  final String? name;

  const AvatarWidget({
    super.key,
    this.imageUrl,
    this.size = 48,
    this.name,
  });

  Color _getBackgroundColor(String name) {
    final int hash = name.hashCode;
    final List<Color> colors = [
      Colors.blue[400]!,
      Colors.orange[400]!,
      Colors.green[400]!,
      Colors.pink[400]!,
      Colors.purple[400]!,
      Colors.teal[400]!,
    ];
    return colors[hash.abs() % colors.length];
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      width: size,
      height: size,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.08),
            blurRadius: 8,
            offset: const Offset(0, 2),
          ),
        ],
        border: Border.all(
          color: Colors.white,
          width: 2,
        ),
      ),
      child: ClipOval(
        child: (imageUrl != null && imageUrl!.startsWith('http'))
            ? Image.network(
                imageUrl!,
                fit: BoxFit.cover,
                errorBuilder: (context, error, stackTrace) {
                  return _buildFallbackAvatar();
                },
              )
            : _buildFallbackAvatar(),
      ),
    );
  }

  Widget _buildFallbackAvatar() {
    final bgColor = name != null ? _getBackgroundColor(name!) : Colors.grey[400]!;
    return Container(
      color: bgColor,
      child: Center(
        child: Text(
          name != null && name!.isNotEmpty
              ? name!.substring(0, 1).toUpperCase()
              : '?',
          style: TextStyle(
            fontSize: size * 0.45,
            color: Colors.white,
            fontWeight: FontWeight.bold,
          ),
        ),
      ),
    );
  }
}
