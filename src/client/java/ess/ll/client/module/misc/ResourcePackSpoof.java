package ess.ll.client.module.misc;

import ess.ll.client.module.ClientModules;
import ess.ll.client.setting.Settings.*;

/**
 * ResourcePackSpoof (from Zenith): the server's resource pack is never downloaded, but the server is told it was
 * accepted and loaded, so servers that kick for a declined pack let you in with your own textures. The hook is
 * ResourcePackSpoofMixin.
 */
public final class ResourcePackSpoof {
    public static final Setting[] SETTINGS={};

    private ResourcePackSpoof() {}

    public static boolean active() { return ClientModules.RESOURCE_PACK_SPOOF!=null && ClientModules.RESOURCE_PACK_SPOOF.enabled(); }
}
