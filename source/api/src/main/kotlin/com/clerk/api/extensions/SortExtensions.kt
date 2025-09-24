package com.clerk.api.extensions

fun List<String>.sortedByPriority(priorityOrder: List<String>): List<String> {
    return this.sortedBy { item ->
        priorityOrder.indexOf(item).takeIf { it != -1 } ?: Int.MAX_VALUE
    }
}
