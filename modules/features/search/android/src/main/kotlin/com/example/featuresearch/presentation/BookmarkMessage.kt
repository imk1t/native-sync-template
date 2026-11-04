package com.example.featuresearch

import com.example.corelocalization.Message
import com.example.sharedbookmarks.BookmarkError

internal fun bookmarkMessage(error: Exception, fallback: Message): Message = when (error as? BookmarkError) {
    is BookmarkError.Limit -> Message.ErrorBookmarkLimit
    is BookmarkError.NoteTooLong -> Message.ErrorNoteTooLong
    is BookmarkError.NotFound -> Message.ErrorBookmarkNotFound
    is BookmarkError.UnsupportedVersion -> Message.ErrorStorageVersion
    null -> fallback
}
