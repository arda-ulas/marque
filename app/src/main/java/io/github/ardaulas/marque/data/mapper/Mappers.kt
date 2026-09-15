package io.github.ardaulas.marque.data.mapper

import io.github.ardaulas.marque.data.local.entity.MakeEntity
import io.github.ardaulas.marque.data.local.entity.ModelEntity
import io.github.ardaulas.marque.data.remote.dto.MakeDto
import io.github.ardaulas.marque.data.remote.dto.ModelDto
import io.github.ardaulas.marque.domain.model.Make
import io.github.ardaulas.marque.domain.model.Model

// DTO -> entity. Both lists are de-duplicated by id defensively: vPIC can repeat a MakeId in
// the makes list, and a duplicate primary key would otherwise make the upsert order-dependent.

fun List<MakeDto>.toEntities(): List<MakeEntity> = distinctBy { it.id }.map { MakeEntity(id = it.id, name = it.name) }

fun List<ModelDto>.toEntities(makeId: Int): List<ModelEntity> =
    distinctBy { it.id }.map { ModelEntity(id = it.id, makeId = makeId, name = it.name) }

// Entity -> domain.

fun MakeEntity.toDomain(): Make = Make(id = id, name = name)

fun ModelEntity.toDomain(): Model = Model(id = id, name = name)
