#include "audio_processor.h"
#include <cstring>

namespace {

void designBandpass(float fc, float fs, float Q, AudioProcessor::BiquadCoef* out) {
    float w0 = 2.0f * 3.14159265f * fc / fs;
    float cw = std::cos(w0);
    float sw = std::sin(w0);
    float alpha = sw / (2.0f * Q);
    float a0 = 1.0f + alpha;
    out->b0 = alpha / a0;
    out->b1 = 0.0f;
    out->b2 = -alpha / a0;
    out->a1 = -2.0f * cw / a0;
    out->a2 = (1.0f - alpha) / a0;
}

}  // namespace

AudioProcessor::AudioProcessor()
    : masterGain_(1.0f)
    , expansionThresholdDb_(-60.0f)
    , expansionRatio_(2.0f)
    , compressionAttackMs_(5.0f)
    , compressionReleaseMs_(50.0f)
    , compressionRatio_(3.0f)
    , compressionThresholdDb_(-40.0f)
    , compressionKneeDb_(10.0f)
    , makeupGainDb_(12.0f)
    , boostQuietSounds_(0.5f)
    , sampleRate_(48000) {
    for (int i = 0; i < NUM_BANDS; ++i) {
        bandGains_[i] = 1.0f;
        prescribedGainsDb_[i] = 0.0f;
        bandCompressionThresholdsDb_[i] = -40.0f;
        bandState_[i] = {0.0f, 0.0f, 0.0f, 0.0f, 0.0f};
        cachedCoefs_[i] = {0.0f, 0.0f, 0.0f, 0.0f, 0.0f};
    }
}

float AudioProcessor::dbToLinear(float db) const {
    return std::pow(10.0f, db / 20.0f);
}

float AudioProcessor::linearToDb(float linear) {
    if (linear <= 1e-10f) return -100.0f;
    return 20.0f * std::log10(linear);
}

void AudioProcessor::setSampleRate(int32_t sampleRate) {
    if (sampleRate > 0) {
        sampleRate_ = sampleRate;
        float fs = static_cast<float>(sampleRate_);
        const float Q = 1.5f;
        for (int b = 0; b < NUM_BANDS; ++b) {
            designBandpass(BAND_FREQS[b], fs, Q, &cachedCoefs_[b]);
        }
    }
}

void AudioProcessor::setMasterGain(float gain) {
    masterGain_ = gain;
}

void AudioProcessor::setBandGain(int band, float linearGain) {
    if (band >= 0 && band < NUM_BANDS) bandGains_[band] = linearGain;
}

void AudioProcessor::setGainsFromDb(const float* gainsDb, int count) {
    for (int i = 0; i < count && i < NUM_BANDS; ++i) {
        bandGains_[i] = dbToLinear(gainsDb[i]);
    }
}

void AudioProcessor::setCompressionParams(float attackMs, float releaseMs, float ratio,
                                         float thresholdDb, float kneeDb) {
    compressionAttackMs_ = attackMs;
    compressionReleaseMs_ = releaseMs;
    compressionRatio_ = ratio;
    compressionThresholdDb_ = thresholdDb;
    compressionKneeDb_ = kneeDb;
}

void AudioProcessor::setBandCompressionThresholds(const float* thresholdsDb, int count) {
    for (int i = 0; i < count && i < NUM_BANDS; ++i) {
        bandCompressionThresholdsDb_[i] = thresholdsDb[i];
    }
}

void AudioProcessor::setPrescribedGains(const float* gainsDb, int count) {
    for (int i = 0; i < count && i < NUM_BANDS; ++i) {
        prescribedGainsDb_[i] = std::min(40.0f, gainsDb[i]);
    }
}

void AudioProcessor::setExpansionParams(float thresholdDb, float ratio) {
    expansionThresholdDb_ = thresholdDb;
    expansionRatio_ = ratio;
}

void AudioProcessor::setBoostQuietSounds(float percent) {
    boostQuietSounds_ = std::max(0.0f, std::min(1.0f, percent));
}

void AudioProcessor::updateFilterCoefficients() {
    // Coefficients are computed per-sample in process() for now, or we could cache them.
    // For efficiency we'll compute in process when sample rate is known.
}

float AudioProcessor::computeCompressionGainDb(float levelDb, int band) const {
    const float expThr = expansionThresholdDb_;
    const float compThr = band < NUM_BANDS ? bandCompressionThresholdsDb_[band] : compressionThresholdDb_;
    const float knee = compressionKneeDb_;

    // Scale ratio and makeup by boostQuietSounds: 0% -> 1.5:1 low makeup, 100% -> 3:1 moderate makeup
    const float ratio = 1.5f + boostQuietSounds_ * 1.5f;
    const float makeup = boostQuietSounds_ * 20.0f;

    // Expansion below expThr: 2:1 ratio (for every 2 dB below, output drops 1 dB more)
    if (levelDb < expThr) {
        float below = expThr - levelDb;
        float expansionGainDb = below / expansionRatio_;
        return -expansionGainDb + makeup;
    }

    // Soft knee around compThr
    float kneeStart = compThr - knee * 0.5f;
    float kneeEnd = compThr + knee * 0.5f;

    if (levelDb < kneeStart) {
        return makeup;
    }
    if (levelDb <= kneeEnd) {
        float t = (levelDb - kneeStart) / knee;
        float fullCompressionGain = makeup - (levelDb - compThr) + (levelDb - compThr) / ratio;
        float noCompressionGain = makeup;
        float smooth = t * t * (3.0f - 2.0f * t);  // smoothstep
        return noCompressionGain + smooth * (fullCompressionGain - noCompressionGain);
    }

    // Above knee: full compression
    float above = levelDb - compThr;
    float compressedDb = above / ratio;
    return makeup - above + compressedDb;
}

float AudioProcessor::processWDRC(float sample) {
    if (sampleRate_ <= 0) return sample;

    float fs = static_cast<float>(sampleRate_);
    float alphaAttack = 1.0f - std::exp(-1.0f / (compressionAttackMs_ * 0.001f * fs));
    float alphaRelease = 1.0f - std::exp(-1.0f / (compressionReleaseMs_ * 0.001f * fs));

    float out = 0.0f;
    for (int b = 0; b < NUM_BANDS; ++b) {
        const BiquadCoef& coef = cachedCoefs_[b];

        BandState& s = bandState_[b];
        float x = sample;
        float y = coef.b0 * x + coef.b1 * s.x1 + coef.b2 * s.x2 - coef.a1 * s.y1 - coef.a2 * s.y2;
        s.x2 = s.x1;
        s.x1 = x;
        s.y2 = s.y1;
        s.y1 = y;

        float envIn = std::abs(y);
        if (envIn > s.env)
            s.env += alphaAttack * (envIn - s.env);
        else
            s.env += alphaRelease * (envIn - s.env);

        float levelDb = linearToDb(s.env + 1e-10f);
        float gainDb = computeCompressionGainDb(levelDb, b) + prescribedGainsDb_[b] + linearToDb(bandGains_[b]);
        gainDb = std::min(30.0f, gainDb);
        float linearGain = dbToLinear(gainDb);
        linearGain = std::min(linearGain, 20.0f);
        out += y * linearGain;
    }

    out /= (NUM_BANDS * 0.5f);
    return out;
}

void AudioProcessor::process(int16_t* buffer, int32_t numFrames) {
    for (int32_t i = 0; i < numFrames; ++i) {
        float sample = buffer[i] / 32768.0f;

        // 1. WDRC (expansion + compression + prescribed gains + band gains)
        sample = processWDRC(sample);

        // 4. Master gain
        sample *= masterGain_;

        // 4. Clip
        // Soft limiter — prevents harsh clipping
        if (sample > 0.5f || sample < -0.5f) {
            sample = std::tanh(sample);
        }

        buffer[i] = static_cast<int16_t>(sample * 32767.0f);
    }
}
