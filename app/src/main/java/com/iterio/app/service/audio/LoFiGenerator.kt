package com.iterio.app.service.audio

import java.util.Random
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

/**
 * Lo-Fi hip hop beat generator with 4 layers:
 *
 * Layer 1 - Chord pad: triangle waves with odd harmonics, bass-separated voicing
 *           (C3+E4G4C5 etc), BiquadLPF@2kHz, +-0.5-1.5Hz detune for chorus,
 *           50ms crossfade between chords
 * Layer 2 - Drums: 75BPM 4/4, kick (sine 150->50Hz sweep 80ms),
 *           snare (1-4kHz noise 50ms), hi-hat (6-12kHz noise 20ms)
 * Layer 3 - Vinyl crackle: 30-60 sparse impulses/sec, very low volume, random stereo pan
 * Layer 4 - Tape wobble: 0.3-0.5Hz LFO pitch +-5 cents, 0.2Hz LFO amplitude 3-5%
 */
class LoFiGenerator(private val sampleRate: Int) : AudioGenerator {
    private val random = Random()
    @Volatile private var volume: Float = 0.5f
    private var sampleIndex: Long = 0

    // ---- Layer 1: Chord Pad ----
    // Chord progressions with bass-separated voicing: [bass, mid1, mid2, high]
    private val chordProgressions = arrayOf(
        floatArrayOf(130.81f, 329.63f, 392.00f, 523.25f), // C3, E4, G4, C5
        floatArrayOf(110.00f, 261.63f, 329.63f, 440.00f), // A2, C4, E4, A4
        floatArrayOf(87.31f, 220.00f, 261.63f, 349.23f),  // F2, A3, C4, F4
        floatArrayOf(73.42f, 174.61f, 220.00f, 293.66f)   // D2, F3, A3, D4
    )

    // Phase accumulators for each voice (4 notes)
    private val phases = FloatArray(4)
    // Detune amounts per voice (random +-0.5-1.5Hz)
    private val detune = FloatArray(4) { (random.nextFloat() * 2f - 1f) * 1.0f }

    private val chordLpf = BiquadFilter(sampleRate, BiquadFilter.FilterType.LOWPASS, 2000f, 0.7f)
    private val chordDurationSamples = (sampleRate * 4.0).toInt()  // 4s per chord
    private val crossfadeSamples = (sampleRate * 0.05).toInt()     // 50ms crossfade
    private val totalCycleSamples = chordDurationSamples * chordProgressions.size

    // ---- Layer 2: Drums (75 BPM) ----
    private val bpm = 75f
    private val samplesPerBeat = (sampleRate * 60f / bpm).toInt()
    private val samplesPerBar = samplesPerBeat * 4

    // Kick state
    private var kickPhase = 0f
    private var kickEnv = 0f
    private val kickDecay = exp(-1.0 / (sampleRate * 0.08)).toFloat()  // 80ms

    // Snare state
    private var snareEnv = 0f
    private val snareDecay = exp(-1.0 / (sampleRate * 0.05)).toFloat()  // 50ms
    private val snareBpf = BiquadFilter(sampleRate, BiquadFilter.FilterType.BANDPASS, 2500f, 0.8f)

    // Hi-hat state
    private var hatEnv = 0f
    private val hatDecay = exp(-1.0 / (sampleRate * 0.02)).toFloat()  // 20ms
    private val hatHpf = BiquadFilter(sampleRate, BiquadFilter.FilterType.HIGHPASS, 7000f, 0.8f)

    // ---- Layer 3: Vinyl crackle ----
    private val crackleProbPerSample = 45f / sampleRate  // ~45 crackles/sec

    // ---- Layer 4: Tape wobble ----
    private val wobblePitchFreq = 0.4f   // 0.4Hz pitch LFO
    private val wobbleAmpFreq = 0.2f     // 0.2Hz amplitude LFO
    private val wobblePitchDepth = 0.0003f  // ~5 cents
    private val wobbleAmpDepth = 0.04f     // 4% amplitude modulation

    /**
     * Triangle wave with first 3 odd harmonics for warmth.
     */
    private fun triangleWave(phase: Float): Float {
        val p = phase % 1f
        val fundamental = if (p < 0.5f) 4f * p - 1f else 3f - 4f * p
        // Add 3rd and 5th harmonic at reduced amplitude
        val h3Phase = (phase * 3f) % 1f
        val h3 = if (h3Phase < 0.5f) 4f * h3Phase - 1f else 3f - 4f * h3Phase
        val h5Phase = (phase * 5f) % 1f
        val h5 = if (h5Phase < 0.5f) 4f * h5Phase - 1f else 3f - 4f * h5Phase
        return fundamental * 0.8f + h3 * 0.12f + h5 * 0.05f
    }

    override fun generate(buffer: ShortArray): Int {
        val amp = volume * 0.3f * Short.MAX_VALUE
        val frames = buffer.size / 2

        for (i in 0 until frames) {
            // ---- Layer 4: Tape wobble (applied globally) ----
            val t = sampleIndex.toFloat() / sampleRate
            val wobblePitch = 1f + wobblePitchDepth * sin(2.0 * PI * wobblePitchFreq * t).toFloat()
            val wobbleAmp = 1f - wobbleAmpDepth + wobbleAmpDepth * sin(2.0 * PI * wobbleAmpFreq * t).toFloat()

            // ---- Layer 1: Chord Pad ----
            val cyclePos = (sampleIndex % totalCycleSamples).toInt()
            val chordIndex = cyclePos / chordDurationSamples
            val posInChord = cyclePos % chordDurationSamples
            val chord = chordProgressions[chordIndex]

            // Crossfade factor
            val xfade = if (posInChord < crossfadeSamples) {
                posInChord.toFloat() / crossfadeSamples
            } else {
                1f
            }

            var chordSample = 0f
            for (v in chord.indices) {
                val freq = (chord[v] + detune[v]) * wobblePitch
                phases[v] = (phases[v] + freq / sampleRate) % 1f
                val voiceSample = triangleWave(phases[v])
                // Bass voice slightly louder
                val voiceGain = if (v == 0) 0.35f else 0.22f
                chordSample += voiceSample * voiceGain
            }

            // Apply crossfade (fade in from previous chord)
            chordSample *= xfade

            // Lowpass filter for warmth
            chordSample = chordLpf.process(chordSample)

            // ---- Layer 2: Drums ----
            val barPos = (sampleIndex % samplesPerBar).toInt()
            val beatPos = barPos % samplesPerBeat

            // Trigger drums at beat boundaries
            if (beatPos == 0) {
                val beatNum = barPos / samplesPerBeat
                // Kick: beats 0 and 2
                if (beatNum == 0 || beatNum == 2) {
                    kickEnv = 1f
                    kickPhase = 0f
                }
                // Snare: beats 1 and 3
                if (beatNum == 1 || beatNum == 3) {
                    snareEnv = 0.7f
                }
                // Hi-hat: every beat
                hatEnv = 0.4f
            }
            // Hi-hat also on eighth notes (halfway between beats)
            if (beatPos == samplesPerBeat / 2) {
                hatEnv = 0.25f
            }

            // Kick: sine sweep 150->50Hz
            var kickSample = 0f
            if (kickEnv > 0.001f) {
                val kickFreq = 50f + 100f * kickEnv  // sweep down as envelope decays
                kickPhase += kickFreq / sampleRate
                if (kickPhase > 1f) kickPhase -= 1f
                kickSample = sin(2.0 * PI * kickPhase).toFloat() * kickEnv * 0.5f
                kickEnv *= kickDecay
            }

            // Snare: bandpass noise
            var snareSample = 0f
            if (snareEnv > 0.001f) {
                val snareNoise = (random.nextFloat() * 2f - 1f) * snareEnv
                snareSample = snareBpf.process(snareNoise) * 0.35f
                snareEnv *= snareDecay
            }

            // Hi-hat: highpass noise
            var hatSample = 0f
            if (hatEnv > 0.001f) {
                val hatNoise = (random.nextFloat() * 2f - 1f) * hatEnv
                hatSample = hatHpf.process(hatNoise) * 0.2f
                hatEnv *= hatDecay
            }

            val drums = kickSample + snareSample + hatSample

            // ---- Layer 3: Vinyl crackle ----
            var crackleL = 0f
            var crackleR = 0f
            if (random.nextFloat() < crackleProbPerSample) {
                val crackleAmp = (0.02f + random.nextFloat() * 0.03f)
                val pan = random.nextFloat()
                crackleL = crackleAmp * (1f - pan) * (random.nextFloat() * 2f - 1f)
                crackleR = crackleAmp * pan * (random.nextFloat() * 2f - 1f)
            }

            // ---- Mix all layers ----
            val monoMix = (chordSample + drums) * wobbleAmp
            val mixL = softClip(monoMix + crackleL)
            val mixR = softClip(monoMix + crackleR)

            buffer[i * 2] = (mixL * amp).toInt().coerceIn(
                Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()
            ).toShort()
            buffer[i * 2 + 1] = (mixR * amp).toInt().coerceIn(
                Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()
            ).toShort()

            sampleIndex++
        }
        return buffer.size
    }

    override fun setVolume(volume: Float) {
        this.volume = volume.coerceIn(0f, 1f)
    }
}
