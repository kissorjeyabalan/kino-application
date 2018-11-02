package com.octopod.cinema.kino.controller

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.Throwables
import com.octopod.cinema.kino.converter.ShowConverter
import com.octopod.cinema.kino.dto.ShowDto
import com.octopod.cinema.kino.repository.ShowRepository
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.util.UriComponentsBuilder
import com.octopod.cinema.common.dto.WrappedResponse
import com.octopod.cinema.kino.entity.Show
import io.swagger.annotations.ApiParam
import com.octopod.cinema.common.hateos.Format
import com.octopod.cinema.common.hateos.HalLink
import com.octopod.cinema.common.hateos.HalPage
import org.springframework.http.MediaType
import javax.validation.ConstraintViolationException

@Api(value = "shows", description = "handling of shows")
@RequestMapping(
        path = ["/shows"],
        produces = [(MediaType.APPLICATION_JSON_VALUE)]
)

@RestController
class ShowController {

    @Autowired
    lateinit var repo: ShowRepository

    @ApiOperation("create a new show")
    @PostMapping
    fun createShow(

            @ApiParam("Show dto")
            @RequestBody dto: ShowDto

    ): ResponseEntity<Void> {

        if (dto.movieName == null || dto.cinemaId == null || dto.startTime == null) {
            return ResponseEntity.status(400).build()
        }

        val created = repo.save(Show(dto.startTime!!, dto.movieName!!, dto.cinemaId!!))

        return ResponseEntity.created(
                UriComponentsBuilder
                        .fromPath("/shows/${created.id}")
                        .build()
                        .toUri()
        ).build()
    }

    @ApiOperation("Get all shows")
    @GetMapping(produces = [Format.HAL_V1])
    fun getShows(

            @ApiParam("Page number")
            @RequestParam("page", defaultValue = "1")
            page: String,

            @ApiParam("Limit")
            @RequestParam("limit", defaultValue = "10")
            limit: String,

            @ApiParam("Theater id")
            @RequestParam("theater", required = false)
            theater: String?

    ): ResponseEntity<WrappedResponse<HalPage<ShowDto>>> {

        val pageInt = page.toInt()
        val limitInt = limit.toInt()

        if (pageInt < 1 || limitInt < 1) {
            return ResponseEntity.status(400).body(
                    WrappedResponse<HalPage<ShowDto>>(
                            code = 400,
                            message = "Malformed limit supplied"
                    ).validated()
            )
        }

        val entryList: List<Show>

        if ( theater.isNullOrBlank()) {
            entryList = repo.findAll().toList()
        } else {
            val theaterId: Long
            try {
                theaterId = theater!!.toLong()
            } catch (e: Exception) {
                return ResponseEntity.status(400).build()
            }
            entryList = repo.findAllByCinemaId(theaterId)
        }

        val dto = ShowConverter.transform(entryList, pageInt, limitInt)

        val uriBuilder = UriComponentsBuilder.fromPath("/shows")
        dto._self = HalLink(uriBuilder.cloneBuilder()
                .queryParam("page", page)
                .queryParam("limit", limit)
                .build().toString())

        if (!entryList.isEmpty() && pageInt > 1) {
            dto.previous = HalLink(uriBuilder.cloneBuilder()
                    .queryParam("page", (pageInt - 1).toString())
                    .queryParam("limit", limit)
                    .build().toString())
        }

        if (((pageInt) * limitInt) < entryList.size) {
            dto.next = HalLink(uriBuilder.cloneBuilder()
                    .queryParam("page", (pageInt + 1).toString())
                    .queryParam("limit", limit)
                    .build().toString())
        }

        return ResponseEntity.ok(
                WrappedResponse(
                        code = 200,
                        data = dto
                ).validated()
        )
    }

    @ApiOperation("Get show with specific id")
    @GetMapping(path = ["/{id}"])
    fun getShow(

            @ApiParam("Show id")
            @PathVariable("id")
            id: String

    ): ResponseEntity<WrappedResponse<ShowDto>> {

        val pathId: Long
        try {
            pathId = id.toLong()
        } catch (e: Exception) {
            /*
                invalid id. But here we return 404 instead of 400,
                as in the API we defined the id as string instead of long
             */
            return ResponseEntity.status(404).build()
        }

        val entity = repo.findById(pathId).orElse(null) ?: return ResponseEntity.status(404).build()
        val dto = ShowConverter.transform(entity)

        return ResponseEntity.ok(
                WrappedResponse(
                        code = 200,
                        data = dto
                ).validated()
        )
    }

    @ApiOperation("Delete show with specific id")
    @DeleteMapping(path = ["/{id}"])
    fun deleteShowById(

            @ApiParam("Show id")
            @PathVariable("id")
            id: String

    ) : ResponseEntity<WrappedResponse<ShowDto>> {

        val pathId: Long
        try {
            pathId = id.toLong()
        } catch (e: Exception) {
            /*
                invalid id. But here we return 404 instead of 400,
                as in the API we defined the id as string instead of long
             */
            return ResponseEntity.status(404).build()
        }

        repo.deleteById(pathId)

        return ResponseEntity.status(204).build()
    }

    @ApiOperation("Update the show")
    @PutMapping(path = ["/{id}"])
    fun updateShow(

            @ApiParam("Show id")
            @PathVariable("id")
            id: String,

            @ApiParam("New show dto")
            @RequestBody
            dto: ShowDto

    ) : ResponseEntity<WrappedResponse<ShowDto>> {

        val pathId: Long
        val dtoId: Long
        try {
            pathId = id.toLong()
            dtoId = dto.id!!.toLong()
        } catch (e: Exception) {
            /*
                invalid id. But here we return 404 instead of 400,
                as in the API we defined the id as string instead of long
             */
            return ResponseEntity.status(404).build()
        }

        if (dtoId != pathId) {
            return ResponseEntity.status(409).build()
        }

        if (!repo.existsById(dtoId)) {
            return ResponseEntity.status(404).build()
        }

        if (dto.startTime == null || dto.cinemaId == null || dto.movieName == null) {
            return ResponseEntity.status(400).build()
        }

        val show = ShowConverter.transform(dto)

        try {
            repo.save(show)
        } catch (e: Exception) {
            if(Throwables.getRootCause(e) is ConstraintViolationException) {
                return ResponseEntity.status(400).build()
            }
            throw e
        }

        return ResponseEntity.status(204).build()
    }

    @ApiOperation("Update a field in show")
    @PatchMapping(path = ["/{id}"])
    fun patchShow(

            @ApiParam("Show id")
            @PathVariable("id")
            id: String,

            @ApiParam("New show JSON")
            @RequestBody
            json: String

    ) : ResponseEntity<WrappedResponse<ShowDto>> {

        val pathId: Long
        try {
            pathId = id.toLong()
        } catch (e: Exception) {
            /*
                invalid id. But here we return 404 instead of 400,
                as in the API we defined the id as string instead of long
             */
            return ResponseEntity.status(404).build()
        }

        val originalDto = repo.findById(pathId).orElse(null) ?: return ResponseEntity.status(404).body(
                WrappedResponse<ShowDto>(
                        code = 404,
                        message = "Resource not found"
                ).validated()
        )

        //taken from arcuri82 CounterRestPatch
        val mapper = ObjectMapper()

        val jsonNode: JsonNode
        try {
            jsonNode = mapper.readValue(json, JsonNode::class.java)
        } catch (e: Exception) {
            //Invalid JSON data as input
            return ResponseEntity.status(400).build()
        }

        if (jsonNode.has("id")) {
            //shouldn't be allowed to modify the counter id
            return ResponseEntity.status(409).build()
        }

        var newStartTime = originalDto.startTime
        var newCinemaName = originalDto.cinemaId
        var newMovieName = originalDto.movieName

        if (jsonNode.has("startTime")) {
            val startTimeNode = jsonNode.get("startTime")
            newStartTime = when {
                startTimeNode.isNull -> return ResponseEntity.status(400).build()
                startTimeNode.isTextual -> startTimeNode.asInt()
                else -> //Invalid JSON. Non-string name
                    return ResponseEntity.status(400).build()
            }
        }

        if (jsonNode.has("cinemaId")) {
            val cinemaNameNode = jsonNode.get("cinemaId")
            newCinemaName = when {
                cinemaNameNode.isNull -> return ResponseEntity.status(400).build()
                cinemaNameNode.isTextual -> cinemaNameNode.asText()
                else -> //Invalid JSON. Non-string name
                    return ResponseEntity.status(400).build()
            }
        }

        if (jsonNode.has("movieName")) {
            val movieNameNode = jsonNode.get("movieName")
            newMovieName = when {
                movieNameNode.isNull -> return ResponseEntity.status(400).build()
                movieNameNode.isTextual -> movieNameNode.asText()
                else -> //Invalid JSON. Non-string name
                    return ResponseEntity.status(400).build()
            }
        }

        originalDto.startTime = newStartTime
        originalDto.cinemaId = newCinemaName
        originalDto.movieName = newMovieName

        repo.save(originalDto)
        return ResponseEntity.status(204).build()
    }
}