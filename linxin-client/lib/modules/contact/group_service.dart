import 'package:lin_xin/modules/contact/group.dart';
import 'package:lin_xin/modules/contact/group_member.dart';
import 'package:lin_xin/core/service/http_service.dart';

class GroupService {
  static GroupService? _instance;
  static GroupService get instance => _instance ??= GroupService._();

  GroupService._();

  final HttpService _httpService = HttpService();

  Future<Group> createGroup({
    required String name,
    String? avatar,
    List<String>? memberIds,
  }) async {
    final response = await _httpService.post('/group/create', data: {
      'name': name,
      'avatar': avatar,
      'memberIds': memberIds,
    }) as Map<String, dynamic>;
    return Group.fromJson(response);
  }

  Future<Group> getGroupInfo(String groupId) async {
    final response = await _httpService.get('/group/$groupId') as Map<String, dynamic>;
    return Group.fromJson(response);
  }

  Future<List<GroupMember>> getGroupMembers(String groupId) async {
    final response = await _httpService.get('/group/$groupId/members') as List;
    return response
        .map((json) => GroupMember.fromJson(json as Map<String, dynamic>))
        .toList();
  }

  Future<Group> addMembers({
    required String groupId,
    required List<String> memberIds,
  }) async {
    final response = await _httpService.post(
      '/group/$groupId/members/add',
      data: {'memberIds': memberIds},
    ) as Map<String, dynamic>;
    return Group.fromJson(response);
  }

  Future<void> removeMember({
    required String groupId,
    required String memberId,
  }) async {
    await _httpService.delete('/group/$groupId/members/$memberId');
  }

  Future<void> leaveGroup(String groupId) async {
    await _httpService.post('/group/$groupId/leave');
  }

  Future<void> dissolveGroup(String groupId) async {
    await _httpService.delete('/group/$groupId');
  }

  Future<void> updateAnnouncement({
    required String groupId,
    required String announcement,
  }) async {
    await _httpService.put(
      '/group/$groupId/announcement',
      data: announcement,
    );
  }

  Future<List<Group>> getMyGroups() async {
    final response = await _httpService.get('/group/my') as List;
    return response
        .map((json) => Group.fromJson(json as Map<String, dynamic>))
        .toList();
  }
}
