package com.example.sharedbookmarks

sealed class BookmarkError : Exception() {
    class Limit : BookmarkError()
    class NoteTooLong : BookmarkError()
    class NotFound : BookmarkError()
    class UnsupportedVersion : BookmarkError()
}
