package com.promethean.proxy.validation


public fun Int.validPort(): Boolean {
    return this in 1..65535
}

public fun String.isValidAddress(): Boolean {
    val ipRegex = Regex("""^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$""")
    val domainRegex = Regex("""^[a-zA-Z0-9][a-zA-Z0-9-]+\.[a-zA-Z]{2,}$""")
    return matches(ipRegex) || matches(domainRegex)
}