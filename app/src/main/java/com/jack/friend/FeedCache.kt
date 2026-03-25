package com.jack.friend

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object FeedCache {
    private const val PREFS_NAME = "feed_cache_prefs"
    private const val KEY_POSTS = "cached_posts"

    fun savePosts(context: Context, posts: List<FeedPost>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = Gson().toJson(posts)
        prefs.edit().putString(KEY_POSTS, json).apply()
    }

    fun getPosts(context: Context): List<FeedPost> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_POSTS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<FeedPost>>() {}.type
            Gson().fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
