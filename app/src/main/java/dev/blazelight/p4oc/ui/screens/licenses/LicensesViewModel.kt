package dev.blazelight.p4oc.ui.screens.licenses

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Loads license texts from `res/raw` lazily on IO. Texts are cached per
 * resource id for the lifetime of the process via [TEXT_CACHE].
 */
class LicensesViewModel(application: Application) : AndroidViewModel(application) {

    private val _loadingTexts = MutableStateFlow<Set<Int>>(emptySet())
    val loadingTexts: StateFlow<Set<Int>> = _loadingTexts.asStateFlow()

    private val _texts = MutableStateFlow<Map<Int, String>>(TEXT_CACHE.toMap())
    val texts: StateFlow<Map<Int, String>> = _texts.asStateFlow()

    fun loadLicenseText(resId: Int) {
        if (TEXT_CACHE.containsKey(resId)) {
            // Ensure exposure even if loaded by another VM instance.
            if (_texts.value[resId] == null) {
                _texts.value = _texts.value + (resId to (TEXT_CACHE[resId] ?: ""))
            }
            return
        }
        if (_loadingTexts.value.contains(resId)) return
        _loadingTexts.value = _loadingTexts.value + resId
        viewModelScope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    getApplication<Application>().resources.openRawResource(resId)
                        .bufferedReader().use { it.readText() }
                }.getOrElse { error ->
                    "Failed to load license text: ${error.message}"
                }
            }
            TEXT_CACHE[resId] = text
            _texts.value = _texts.value + (resId to text)
            _loadingTexts.value = _loadingTexts.value - resId
        }
    }

    companion object {
        private val TEXT_CACHE = mutableMapOf<Int, String>()
    }
}
