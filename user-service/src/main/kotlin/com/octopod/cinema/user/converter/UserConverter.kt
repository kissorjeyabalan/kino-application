package com.octopod.cinema.user.converter

import com.octopod.cinema.user.entity.UserEntity
import dto.UserDto
import hateos.HalPage
import kotlin.streams.toList

class UserConverter {
    companion object {
        fun transform(entity: UserEntity): UserDto {
            return UserDto(
                    phone = entity.phone.toString(),
                    email = entity.email.toString(),
                    created = entity.creationTime,
                    updated = entity.updatedTime
            )
        }

        fun transform(entities: Iterable<UserEntity>): List<UserDto> {
            return entities.map { transform(it) }
        }

        fun transform(entities: List<UserEntity>, page: Int, limit: Int): HalPage<UserDto> {
            val offset = ((page -1) * limit).toLong()
            val dtoList: MutableList<UserDto> = entities.stream()
                    .skip(offset)
                    .limit(limit.toLong())
                    .map { transform(it) }
                    .toList().toMutableList()

            val pageDto = HalPage<UserDto>()
            pageDto.data = dtoList
            pageDto.count = entities.size.toLong()
            pageDto.pages = ((pageDto.count / limit) +1).toInt()

            return pageDto
        }
    }
}