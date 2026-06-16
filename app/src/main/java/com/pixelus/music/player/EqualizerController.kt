package com.pixelus.music.player

import android.content.Context
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.audiofx.Equalizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private class EqualizerSettings(context: Context) {
    private val prefs = context.getSharedPreferences("equalizer_prefs", Context.MODE_PRIVATE)

    private val isEnabledKey = "is_eq_enabled"
    private val bandFrequenciesKey = "band_frequencies"
    private val lowerLevelLimitKey = "lower_level_limit"
    private val upperLevelLimitKey = "upper_level_limit"
    private val bandLevelsKey = "band_levels"

    var isEnabled: Boolean
        get() = prefs.getBoolean(isEnabledKey, false)
        set(value) { prefs.edit().putBoolean(isEnabledKey, value).apply() }

    var bandFrequencies: List<String>?
        get() = prefs.getString(bandFrequenciesKey, null)?.let { Json.decodeFromString(it) }
        set(value) { prefs.edit().putString(bandFrequenciesKey, value?.let { Json.encodeToString(it) }).apply() }

    var lowerLevelLimit: Int
        get() = prefs.getInt(lowerLevelLimitKey, 0)
        set(value) { prefs.edit().putInt(lowerLevelLimitKey, value).apply() }

    var upperLevelLimit: Int
        get() = prefs.getInt(upperLevelLimitKey, 0)
        set(value) { prefs.edit().putInt(upperLevelLimitKey, value).apply() }

    var bandLevels: List<Short>?
        get() = prefs.getString(bandLevelsKey, null)?.let { Json.decodeFromString(it) }
        set(value) { prefs.edit().putString(bandLevelsKey, value?.let { Json.encodeToString(it) }).apply() }
}

class EqualizerController(context: Context) {
    private val settings = EqualizerSettings(context)
    private var audioSessionId: Int? = null
    var equalizer: Equalizer? = null
        private set

    init {
        firstLaunchInit()
    }

    private val _isEqEnabled = MutableStateFlow(settings.isEnabled)
    val isEqEnabled = _isEqEnabled.asStateFlow()
    fun updateIsEqEnabled(value: Boolean) {
        _isEqEnabled.update { value }
        settings.isEnabled = value
        audioSessionId?.let { updateEqualizer(it) }
    }

    private val _bandFrequencies = MutableStateFlow<List<String>?>(settings.bandFrequencies)
    val bandFrequencies = _bandFrequencies.asStateFlow()

    private val _lowerLevelLimit = MutableStateFlow<Short>(settings.lowerLevelLimit.toShort())
    val lowerLevelLimit = _lowerLevelLimit.asStateFlow()

    private val _upperLevelLimit = MutableStateFlow<Short>(settings.upperLevelLimit.toShort())
    val upperLevelLimit = _upperLevelLimit.asStateFlow()

    private val _bandLevels = MutableStateFlow(settings.bandLevels)
    val bandLevels = _bandLevels.asStateFlow()
    fun updateBandLevels(levels: List<Short>) {
        _bandLevels.update { levels }
        settings.bandLevels = levels
        audioSessionId?.let { updateEqualizer(it) }
    }

    fun updateEqualizer(audioSessionId: Int) {
        equalizer?.release()
        this.audioSessionId = audioSessionId
        if (!settings.isEnabled) return

        equalizer = Equalizer(Int.MAX_VALUE, audioSessionId).apply {
            enabled = true
            settings.bandLevels?.forEachIndexed { band, level ->
                setBandLevel(band.toShort(), level)
            }
        }
    }

    fun releaseEqualizer() {
        equalizer?.release()
        equalizer = null
    }

    fun resetBandLevels() {
        settings.bandLevels?.let { levels ->
            val defaultLevels = List<Short>(levels.size) { 0 }
            settings.bandLevels = defaultLevels
            _bandLevels.update { defaultLevels }
        }
        audioSessionId?.let { updateEqualizer(it) }
    }

    private fun firstLaunchInit() {
        if (settings.bandFrequencies != null && settings.bandLevels != null) return

        try {
            val audioTrack = AudioTrack.Builder()
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(44100)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(
                    AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
                )
                .build()

            Equalizer(0, audioTrack.audioSessionId).run {
                enabled = false
                val defaultLevels = List<Short>(numberOfBands.toInt()) { 0 }
                settings.bandLevels = defaultLevels

                val bandFreqs = List(numberOfBands.toInt()) {
                    getBandFreqRange(it.toShort()).joinToString("-") { (it / 1000).toString() } + "Hz"
                }
                settings.bandFrequencies = bandFreqs

                settings.lowerLevelLimit = bandLevelRange[0].toInt()
                settings.upperLevelLimit = bandLevelRange[1].toInt()

                release()
            }
            audioTrack.release()
        } catch (e: Exception) {
            Log.e("EqualizerInit", "Failed to initialize equalizer defaults", e)
        }
    }
}
