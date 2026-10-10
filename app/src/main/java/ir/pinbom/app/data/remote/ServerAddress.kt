package ir.pinbom.app.data.remote

import okhttp3.HttpUrl
import java.net.URI

/* ────────────────────────────────────────────────────────────────────────────
 *  PinBom · where a call is actually sent
 *
 *  Retrofit is built against a placeholder base URL, because the real server is
 *  not known until the user types it on first launch. Every request is therefore
 *  re-addressed on the way out, and *this* is the function that decides the
 *  destination — pulled out of the interceptor so it can be tested directly
 *  rather than inferred from a log line.
 *
 *  Rules, in full:
 *
 *   · no server configured        → the request keeps the placeholder address;
 *   · server without a host       → same (nothing trustworthy to use);
 *   · otherwise                  → scheme, host and port come from the server,
 *                                  the request's own path is preserved, and a
 *                                  path prefix in the server address (a reverse
 *                                  proxy under /pinbom, say) is kept in front.
 *
 *  The port matters most: leaving the pre-filled address in place on a phone
 *  pointed every call at the phone itself, and a server on a non-default port
 *  looked unreachable when the port was simply being dropped.
 * ──────────────────────────────────────────────────────────────────────────── */

internal fun resolveServerUrl(base: String?, url: HttpUrl): HttpUrl? {
    val raw = base?.trim()?.trimEnd('/')
    if (raw.isNullOrBlank()) return null

    val target = try {
        URI(raw)
    } catch (_: Exception) {
        return null
    }
    val host = target.host
    if (host.isNullOrBlank()) return null

    val prefix = target.rawPath.orEmpty().trimEnd('/')

    return url.newBuilder()
        .scheme(target.scheme ?: url.scheme)
        .host(host)
        .apply { if (target.port > 0) port(target.port) }
        .apply { if (prefix.isNotEmpty()) encodedPath(prefix + url.encodedPath) }
        .build()
}
