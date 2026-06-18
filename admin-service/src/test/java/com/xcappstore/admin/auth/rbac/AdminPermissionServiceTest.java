package com.xcappstore.admin.auth.rbac;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.xcappstore.admin.auth.AdminPrincipal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AdminPermissionServiceTest {
    @Test
    void grantsExactAndWildcardPermissions() {
        FakePermissionMapper mapper = new FakePermissionMapper();
        mapper.permissions.put(1L, List.of("software:view", "review:*"));
        AdminPermissionService service = new AdminPermissionService(mapper);
        AdminPrincipal principal = new AdminPrincipal(1L, "operator", "admin", 0L);

        assertEquals(true, service.hasPermission(principal, "software:view"));
        assertEquals(true, service.hasPermission(principal, "review:approve"));
        assertEquals(false, service.hasPermission(principal, "software:publish"));
    }

    @Test
    void grantsAllPermissionsForSuperAdminWildcard() {
        FakePermissionMapper mapper = new FakePermissionMapper();
        mapper.permissions.put(1L, List.of("*"));
        AdminPermissionService service = new AdminPermissionService(mapper);

        assertEquals(true, service.hasPermission(new AdminPrincipal(1L, "admin", "admin", 0L), "tag:manage"));
    }

    @Test
    void rejectsUnknownUserAndNonAdminPrincipal() {
        FakePermissionMapper mapper = new FakePermissionMapper();
        AdminPermissionService service = new AdminPermissionService(mapper);

        assertEquals(false, service.hasPermission(new AdminPrincipal(2L, "viewer", "admin", 0L), "software:view"));
        assertEquals(false, service.hasPermission(new AdminPrincipal(1L, "client", "client", 0L), "software:view"));
    }

    private static final class FakePermissionMapper implements AdminPermissionMapper {
        private final Map<Long, List<String>> permissions = new HashMap<>();

        @Override
        public List<String> selectPermissionCodesByUserId(Long userId) {
            return permissions.getOrDefault(userId, List.of());
        }
    }
}
