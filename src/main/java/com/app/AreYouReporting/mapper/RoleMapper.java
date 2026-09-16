package com.app.AreYouReporting.mapper;

import com.app.AreYouReporting.Entities.Role;
import com.app.AreYouReporting.payload.response.RoleDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RoleMapper {

    private final PermissionMapper permissionMapper;

    public RoleDto toDto(Role role) {
        if (role == null) return null;
        return RoleDto.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .isSystemRole(role.isSystemRole())
                .defaultDataScope(role.getDefaultDataScope())
                .permissions(permissionMapper.toDtoList(role.getPermissions()))
                .build();
    }

    public List<RoleDto> toDtoList(Collection<Role> roles) {
        if (roles == null) return Collections.emptyList();
        return roles.stream().map(this::toDto).collect(Collectors.toList());
    }
}
