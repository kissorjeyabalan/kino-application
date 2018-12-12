package no.octopod.cinema.common.dto

import java.time.ZonedDateTime

data class OrderDto(
        var id: Long? = null,
        var order_time: ZonedDateTime? = null,
        var user_id: String? = null,
        var price: Int? = null,
        var screening_id: Long? = null,
        var payment_token: String? = null,
        var tickets: List<Long>? = null
)