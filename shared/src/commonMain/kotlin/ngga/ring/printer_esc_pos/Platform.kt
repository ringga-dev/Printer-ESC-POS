package ngga.ring.printer_esc_pos

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform