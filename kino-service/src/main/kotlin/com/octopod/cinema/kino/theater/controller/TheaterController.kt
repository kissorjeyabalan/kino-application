package com.octopod.cinema.kino.show.controller

import com.octopod.cinema.kino.show.converter.TheaterConverter
import com.octopod.cinema.kino.show.dto.TheaterDto
import com.octopod.cinema.kino.show.service.TheaterService
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.util.UriComponentsBuilder
import dto.WrappedResponse

@Api(value = "theater", description = "Handling theaters")
@RequestMapping(
        path = ["/theater"]
)
@RestController
class TheaterController {

    @Autowired
    private lateinit var service: TheaterService

    @ApiOperation("create a new ticket")
    @PostMapping
    fun createTheater(

            @RequestBody dto: TheaterDto

    ): ResponseEntity<Void> {

        if (dto.id == null || dto.name == null || dto.seatsMax == null) {
            return ResponseEntity.status(400).build()
        }

        val id = service.createTheater(dto.name!!, dto.seatsMax!!, dto.seatsMax!!)

        return ResponseEntity.created(
                UriComponentsBuilder
                        .fromPath("/theaters/$id")
                        .build()
                        .toUri()
        ).build()
    }

    @ApiOperation("Get all theaters")
    @GetMapping
    fun getTheaters(

        @RequestParam("limit", defaultValue = "10")
        limit: Int

    ): ResponseEntity<WrappedResponse<List<TheaterDto>>> {

        if (limit < 1) {
            return ResponseEntity.status(400).body(
                    WrappedResponse<List<TheaterDto>>(
                            code = 400,
                            message = "Malformed limit supplied"
                    ).validated()
            )
        }

        val entryList = service.getTheaters(limit).toList()
        val dto = TheaterConverter.transform(entryList, limit)

        return ResponseEntity.ok(
                WrappedResponse(
                        code = 200,
                        data = dto
                ).validated()
        )
    }

    @ApiOperation("Get ticket with specific id")
    @GetMapping
    fun getTheater(

            @RequestParam("id")
            id: String

    ): ResponseEntity<WrappedResponse<TheaterDto>> {

        //Finne ut om dette er riktig måte å gjøre det fra long til string og omvendt
        val entryObject = service.getTheater(id.toLong())
        val dto = TheaterConverter.transform(entryObject)

        return ResponseEntity.ok(
                WrappedResponse(
                        code = 200,
                        data = dto
                ).validated()
        )
    }
}