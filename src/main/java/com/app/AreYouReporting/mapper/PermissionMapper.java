package com.app.AreYouReporting.mapper;

import com.app.AreYouReporting.Entities.Permission;
import com.app.AreYouReporting.Entities.Role;
import com.app.AreYouReporting.payload.response.PermissionDto;
import com.app.AreYouReporting.payload.response.PermissionGrantItemDto;
import com.app.AreYouReporting.payload.response.ResourceGrantsDto;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class PermissionMapper {

    public PermissionDto toDto(Permission permission) {
        if (permission == null) return null;
        return PermissionDto.builder()
                .id(permission.getId())
                .resource(permission.getResource())
                .action(permission.getAction())
                .authority(permission.getAuthority())
                .description(permission.getDescription())
                .isSystemPermission(permission.isSystemPermission())
                .build();
    }

    public List<PermissionDto> toDtoList(Collection<Permission> permissions) {
        if (permissions == null) return Collections.emptyList();
        return permissions.stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<ResourceGrantsDto> toResourceGrants(List<Permission> allPermissions, Set<Permission> grantedPermissions) {
        Set<UUID> grantedIds = grantedPermissions != null
                ? grantedPermissions.stream().map(Permission::getId).collect(Collectors.toSet())
                : Collections.emptySet();

        Map<String, List<Permission>> grouped = allPermissions.stream()
                .collect(Collectors.groupingBy(Permission::getResource, LinkedHashMap::new, Collectors.toList()));

        List<ResourceGrantsDto> result = new ArrayList<>();
        for (Map.Entry<String, List<Permission>> entry : grouped.entrySet()) {
            List<PermissionGrantItemDto> items = entry.getValue().stream()
                    .map(p -> PermissionGrantItemDto.builder()
                            .permissionId(p.getId())
                            .action(p.getAction())
                            .authority(p.getAuthority())
                            .granted(grantedIds.contains(p.getId()))
                            .build())
                    .collect(Collectors.toList());

            result.add(ResourceGrantsDto.builder()
                    .resource(entry.getKey())
                    .permissions(items)
                    .build());
        }
        return result;
    }
}
