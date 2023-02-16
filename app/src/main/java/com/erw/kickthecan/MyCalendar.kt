package com.erw.kickthecan

data class MyCalendar(
    val id: Long = -1,
    val name: String? = "",
    val displayName: String? = "",
    val color: Int? = -1,
    val visible: Boolean? = false,
    val syncEvents: Boolean? = false,
    val accountName: String? = "",
    val accountType: String? = "",
)
