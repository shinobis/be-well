package com.bewell.plugin

import com.bewell.api.WellnessApi
import net.corda.webserver.services.WebServerPluginRegistry
import java.util.function.Function

/**
 * This plugin registers the web apis for the application.
 */
class WellnessPlugin : WebServerPluginRegistry {
    override val webApis = listOf(Function(::WellnessApi))
}