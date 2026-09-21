package com.annotation.catalogfilecomment.service

import com.google.gson.Gson
import com.intellij.openapi.application.PathManager
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.TimeUnit

class CloudRuleService {
    // Cloud rule source: a GitHub Gist maintained by the plugin author.
    // The unpinned "raw" URL always serves the latest revision; rules are cached
    // on disk for 24 hours so GitHub rate limits are not a problem in practice.
    private val dataUrl =
        "https://gist.githubusercontent.com/CyanQX/63ab74e15135fd3fd0aec8c6012bd360/raw/CatalogFileComments.json"
    private val cacheFileName = "catalog_rules_cache_v2.json"
    private val cacheDurationHours = 24
    private var cachedRules: Map<String, String> = emptyMap()
    private var lastFetchTime: Long = 0

    init {
        if (System.getProperty("catalogfilecomment.forcerefresh") == "true") {
            println("🔄 Force refresh mode enabled")
            forceRefresh()
        }
    }

    companion object {
        private var instance: CloudRuleService? = null
        fun getInstance(): CloudRuleService {
            if (instance == null) instance = CloudRuleService()
            return instance!!
        }
    }

    fun getRules(): Map<String, String> {
        if (cachedRules.isNotEmpty() && !isCacheExpired()) {
            println("✅ Using in-memory cache")
            return cachedRules
        }
        val localRules = loadFromLocalDisk()
        if (localRules.isNotEmpty()) {
            cachedRules = localRules
            lastFetchTime = File(PathManager.getPluginTempPath(), cacheFileName).lastModified()
            if (!isCacheExpired()) {
                println("✅ Using local disk cache")
                return localRules
            }
        }
        println("🌐 Fetching rules from the cloud...")
        val remoteRules = fetchFromRemote()
        return if (remoteRules.isNotEmpty()) {
            cachedRules = remoteRules
            saveToLocalDisk(remoteRules)
            lastFetchTime = System.currentTimeMillis()
            println("✅ Cloud rules loaded, cache updated")
            remoteRules
        } else {
            println("⚠️ Network request failed, using stale cache")
            localRules
        }
    }

    fun forceRefresh(): Map<String, String> {
        println("🔄 Force refreshing...")
        lastFetchTime = 0
        cachedRules = emptyMap()
        return getRules()
    }

    private fun fetchFromRemote(): Map<String, String> {
        return try {
            val uri = URI(dataUrl)
            val connection = uri.toURL().openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("User-Agent", "CatalogFileComment-Plugin/1.0")
                setRequestProperty("Cache-Control", "max-age=3600")
            }

            val responseCode = connection.responseCode
            if (responseCode == 429) {
                println("❌ GitHub rate limit hit (429), please try again later")
                return emptyMap()
            }

            if (responseCode == 200) {
                val json = connection.inputStream.bufferedReader().use { it.readText() }
                parseJsonToFlatMap(json)
            } else {
                println("❌ HTTP error: $responseCode")
                emptyMap()
            }
        } catch (e: Exception) {
            println("❌ Network request failed: ${e.message}")
            emptyMap()
        }
    }

    private fun loadFromLocalDisk(): Map<String, String> {
        return try {
            val file = File(PathManager.getPluginTempPath(), cacheFileName)
            if (file.exists()) {
                val json = file.readText()
                val type = object : com.google.gson.reflect.TypeToken<Map<String, String>>() {}.type
                Gson().fromJson(json, type)
            } else {
                emptyMap()
            }
        } catch (e: Exception) {
            println("⚠️ Failed to read local cache: ${e.message}")
            emptyMap()
        }
    }

    private fun saveToLocalDisk(data: Map<String, String>) {
        try {
            val dir = File(PathManager.getPluginTempPath())
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, cacheFileName)
            file.writeText(Gson().toJson(data))
            println("💾 Cache saved to: ${file.absolutePath}")
        } catch (e: Exception) {
            println("⚠️ Failed to save local cache: ${e.message}")
        }
    }

    private fun isCacheExpired(): Boolean {
        val elapsed = System.currentTimeMillis() - lastFetchTime
        return elapsed > TimeUnit.HOURS.toMillis(cacheDurationHours.toLong())
    }

    private fun parseJsonToFlatMap(json: String): Map<String, String> {
        val resultMap = mutableMapOf<String, String>()
        try {
            val data = Gson().fromJson(json, ArchitectureData::class.java)
            data.layers?.forEach { layer ->
                // Prefer the English layer name when the rule source provides one
                val layerName = layer.nameEn?.takeIf { it.isNotBlank() }
                    ?: layer.name
                    ?: "Unknown Layer"
                layer.components?.forEach { component ->
                    val suffix = component.type
                    val desc = component.description
                    if (!suffix.isNullOrBlank()) {
                        resultMap[suffix] = "${layerName}_${desc}"
                    }
                }
            }
        } catch (e: Exception) {
            println("❌ JSON parsing failed: ${e.message}")
        }
        return resultMap
    }

    private data class ArchitectureData(val layers: List<Layer>? = null)
    private data class Layer(
        val name: String? = null,
        val nameEn: String? = null,
        val components: List<Component>? = null
    )
    private data class Component(val type: String? = null, val description: String? = null)
}