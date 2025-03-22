package tw.zipe.bastpartner.config

import io.quarkus.security.spi.runtime.AuthenticationFailureEvent
import io.quarkus.security.spi.runtime.AuthenticationSuccessEvent
import io.quarkus.security.spi.runtime.AuthorizationFailureEvent
import io.quarkus.security.spi.runtime.AuthorizationSuccessEvent
import io.quarkus.security.spi.runtime.SecurityEvent
import io.vertx.ext.web.RoutingContext
import jakarta.enterprise.event.Observes
import jakarta.enterprise.event.ObservesAsync
import tw.zipe.bastpartner.util.logger

/**
 * 觀察權限事件
 */
class SecurityEventObserver {

    private val logger = logger()

    fun observeAuthenticationSuccess(@ObservesAsync event: AuthenticationSuccessEvent) {
        logger.debug("User ${event.securityIdentity.principal.name} has authenticated successfully")
    }

    fun observeAuthenticationFailure(@ObservesAsync event: AuthenticationFailureEvent) {
        val routingContext: RoutingContext? =
            event.eventProperties[RoutingContext::class.java.getName()] as RoutingContext?
        logger.error("Authentication failed, request path: ${routingContext?.request()?.path()}")
    }

    fun observeAuthorizationSuccess(@ObservesAsync event: AuthorizationSuccessEvent) {
        val principalName = getPrincipalName(event)
        if (principalName != null) {
            logger.debug("User $principalName has been authorized successfully")
        }
    }

    fun observeAuthorizationFailure(@Observes event: AuthorizationFailureEvent) {
        val routingContext: RoutingContext? =
            event.eventProperties[RoutingContext::class.java.getName()] as RoutingContext?
        logger.error("Authentication failed, request path: ${routingContext?.request()?.path()}")
        logger.error("User ${event.securityIdentity.principal.name} authorization failed")
    }

    private fun getPrincipalName(event: SecurityEvent): String? {
        if (event.securityIdentity != null) {
            return event.securityIdentity.principal.name
        }
        return null
    }
}
