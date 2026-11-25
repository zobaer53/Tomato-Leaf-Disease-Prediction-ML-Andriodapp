package com.zobaer53.plantapp.core.domain.model

/** Opaque, platform-neutral reference to a user-selected image (a URI string on Android). */
@JvmInline
value class ImageRef(val value: String) {
    init {
        require(value.isNotBlank()) { "ImageRef must not be blank" }
    }
}
