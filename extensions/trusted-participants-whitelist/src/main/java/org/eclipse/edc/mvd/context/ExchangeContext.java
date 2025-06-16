package org.eclipse.edc.mvd.context;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores provider & consumer connector base URLs per asset (id → URLs).
 * The context is kept in‑memory because a single trustee instance runs in the container.
 * Persist to external store if you cluster.
 */
public final class ExchangeContext {
    private record Ctx(String provider, String consumer) { }

    private static final Map<String, Ctx> MAP = new ConcurrentHashMap<>();

    private ExchangeContext() { }

    /* ───────────────────────────── public API ──────────────────────────── */

    /** remember where an asset comes from / goes to */
    public static void put(String assetId, String provider, String consumer) {
        MAP.put(assetId, new Ctx(provider, consumer));
    }

    /** return provider base-URL or throw a descriptive error */
    public static String provider(String assetId) {
        Ctx ctx = MAP.get(assetId);
        if (ctx == null) {
            throw new IllegalStateException("No provider URL known for asset " + assetId);
        }
        return ctx.provider();
    }

    /** return consumer base-URL or throw a descriptive error */
    public static String consumer(String assetId) {
        Ctx ctx = MAP.get(assetId);
        if (ctx == null) {
            throw new IllegalStateException("No consumer URL known for asset " + assetId);
        }
        return ctx.consumer();
    }

    /** list *every* asset currently stored in the context */
    public static List<String> allAssets() {
        return MAP.keySet().stream().toList();
    }

    /** list only the assets that belong to the given entry-id */
    public static List<String> assetsOfEntry(String entryId) {
        return MAP.keySet().stream()
                .filter(a -> a.startsWith(entryId + "::"))   // naming convention
                .toList();
    }
}