#ifndef SOUNDAMPLIFIER_AUDIO_PROCESSOR_H
#define SOUNDAMPLIFIER_AUDIO_PROCESSOR_H

#include <cstdint>
#include <cmath>
#include <algorithm>

/**
 * Real-time audio processor with WDRC (Wide Dynamic Range Compression).
 * - Soft expander (noise gate) below expansion threshold
 * - Multi-band WDRC across 6 EQ bands (250Hz–8kHz)
 * - Audiogram-driven prescribed gains
 * - Master gain
 */
class AudioProcessor {
public:
    static constexpr int NUM_BANDS = 6;
    static constexpr float BAND_FREQS[NUM_BANDS] = {250.f, 500.f, 1000.f, 2000.f, 4000.f, 8000.f};

    // Biquad coefficients for bandpass (normalized)
    struct BiquadCoef {
        float b0, b1, b2, a1, a2;
    };

    AudioProcessor();

    /** Process buffer in-place. Samples are int16, mono. */
    void process(int16_t* buffer, int32_t numFrames);

    void setSampleRate(int32_t sampleRate);

    void setMasterGain(float gain);
    void setBandGain(int band, float linearGain);
    void setGainsFromDb(const float* gainsDb, int count);

    // WDRC / compression
    void setCompressionParams(float attackMs, float releaseMs, float ratio, float thresholdDb, float kneeDb);
    void setBandCompressionThresholds(const float* thresholdsDb, int count);
    void setPrescribedGains(const float* gainsDb, int count);
    void setExpansionParams(float thresholdDb, float ratio);
    void setBoostQuietSounds(float percent);  // 0-1, scales makeup gain and compression ratio

private:
    float bandGains_[NUM_BANDS];
    float prescribedGainsDb_[NUM_BANDS];
    float bandCompressionThresholdsDb_[NUM_BANDS];
    float masterGain_;

    // Expansion (below threshold)
    float expansionThresholdDb_;
    float expansionRatio_;

    // Compression
    float compressionAttackMs_;
    float compressionReleaseMs_;
    float compressionRatio_;
    float compressionThresholdDb_;
    float compressionKneeDb_;
    float makeupGainDb_;

    // Boost quiet sounds 0-1: scales ratio (1.5–4) and makeup gain
    float boostQuietSounds_;

    int32_t sampleRate_;

    // Per-band state for WDRC
    struct BandState {
        float env;           // envelope follower state
        float x1, x2, y1, y2; // biquad direct form 1 state
    };
    BandState bandState_[NUM_BANDS];
    BiquadCoef cachedCoefs_[NUM_BANDS];

    float processWDRC(float sample);
    float dbToLinear(float db) const;
    void updateFilterCoefficients();
    float computeCompressionGainDb(float levelDb, int band) const;
    static float linearToDb(float linear);
};

#endif // SOUNDAMPLIFIER_AUDIO_PROCESSOR_H
