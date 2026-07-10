package com.nyberg.notifications.tenant;

import java.util.UUID;

public class OrganizationContext {

    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    public static void set(UUID organizationId) { CURRENT.set(organizationId); }
    public static UUID get()                    { return CURRENT.get(); }
    public static void clear()                  { CURRENT.remove(); }
}
