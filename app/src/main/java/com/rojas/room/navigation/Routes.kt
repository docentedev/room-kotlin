package com.rojas.room.navigation

object Routes {
    const val LIST = "list"
    const val CREATE = "create"
    const val DETAIL = "detail/{id}"
    const val UPDATE = "update/{id}"

    fun detail(id: Long) = "detail/$id"
    fun update(id: Long) = "update/$id"
}
