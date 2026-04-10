package ngga.ring.printer.model

/**
 * Configuration for printer discovery.
 */
data class DiscoveryConfig(
    val showVirtualDevices: Boolean = false,
    val networkScanTimeoutMs: Int = 300
)
