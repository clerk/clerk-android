package com.clerk.api.client

import com.clerk.api.network.model.client.Client

/**
 * Represents client events emitted by Clerk.
 *
 * Subscribe to [com.clerk.api.Clerk.clientEvents] to receive notifications about client state
 * changes.
 */
sealed interface ClientEvent

/**
 * Emitted when the current client changes.
 *
 * This event fires whenever Clerk receives a new [Client] value that differs from the previously
 * stored client. Because [Client] is a data class, nested changes such as a session or user
 * property update also trigger this event.
 *
 * @property client The updated client.
 */
data class ClientChanged(val client: Client) : ClientEvent
