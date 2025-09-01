package com.abmo.util

import com.google.gson.Gson

fun Any.toJson(): String = Gson().toJson(this)

inline fun <reified T> String.toObject(): T {
    return Gson().fromJson(this, T::class.java)
}