#include "oboe_audio_engine.h"
#include <android/log.h>
#include <cstring>
#include <cmath>

#define LOG_TAG "OboeEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

namespace {
constexpr int64_t kReadTimeoutNs = 20 * 1000000;  // 20ms
constexpr int32_t kBufferFrames = 256;
}

OboeAudioEngine::OboeAudioEngine() {
    std::memset(ringBuffer_, 0, sizeof(ringBuffer_));
    std::memset(visBuffer_, 0, sizeof(visBuffer_));
}

OboeAudioEngine::~OboeAudioEngine() {
    stop();
}

oboe::AudioStreamBuilder OboeAudioEngine::createInputBuilder() {
    oboe::AudioStreamBuilder builder;
    builder.setDirection(oboe::Direction::Input)
        ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
        ->setSharingMode(oboe::SharingMode::Exclusive)
        ->setFormat(oboe::AudioFormat::I16)
        ->setChannelCount(oboe::ChannelCount::Mono)
        ->setSampleRateConversionQuality(oboe::SampleRateConversionQuality::Fastest)
        ->setInputPreset(oboe::InputPreset::Unprocessed);
    int32_t preferred = preferredInputDeviceId_.load(std::memory_order_acquire);
    if (preferred > 0) {
        builder.setDeviceId(preferred);
    }
    return builder;
}

oboe::AudioStreamBuilder OboeAudioEngine::createOutputBuilder() {
    oboe::AudioStreamBuilder builder;
    builder.setDirection(oboe::Direction::Output)
        ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
        ->setSharingMode(oboe::SharingMode::Exclusive)
        ->setFormat(oboe::AudioFormat::I16)
        ->setChannelCount(oboe::ChannelCount::Mono)
        ->setSampleRateConversionQuality(oboe::SampleRateConversionQuality::Fastest)
        ->setUsage(oboe::Usage::Assistant)
        ->setContentType(oboe::ContentType::Speech)
        ->setDataCallback(this)
        ->setErrorCallback(this);
    return builder;
}

void OboeAudioEngine::processInputFrames(const int16_t* data, int32_t numFrames) {
    // Update input level (RMS with smoothing)
    float rms = computeRms(data, numFrames);
    float current = inputLevel_.load();
    inputLevel_.store(std::max(current * 0.85f, rms * 3.0f));

    // Copy, process, push
    int16_t buffer[kBufferFrames];
    if (numFrames > kBufferFrames) numFrames = kBufferFrames;
    std::memcpy(buffer, data, numFrames * sizeof(int16_t));
    {
        std::lock_guard<std::mutex> lock(paramMutex_);
        processor_.process(buffer, numFrames);
    }
    for (int32_t i = 0; i < numFrames; ++i) {
        int idx = visWriteIndex_.load(std::memory_order_relaxed);
        visBuffer_[idx] = buffer[i];
        visWriteIndex_.store((idx + 1) % VIS_BUFFER_SIZE, std::memory_order_release);
    }
    pushFrames(buffer, numFrames);
}

float OboeAudioEngine::computeRms(const int16_t* data, int32_t numFrames) {
    if (numFrames == 0) return 0.0f;
    double sum = 0;
    float peak = 0;
    for (int32_t i = 0; i < numFrames; ++i) {
        float s = std::abs(data[i] / 32768.0f);
        sum += s * s;
        if (s > peak) peak = s;
    }
    float rms = static_cast<float>(std::sqrt(sum / numFrames));
    return std::max(rms, peak * 0.5f);
}

int OboeAudioEngine::getAvailableFrames() const {
    int w = writeIndex_.load(std::memory_order_acquire);
    int r = readIndex_.load(std::memory_order_acquire);
    int diff = w - r;
    if (diff < 0) diff += RING_CAPACITY;
    return diff;
}

void OboeAudioEngine::pushFrames(const int16_t* data, int32_t numFrames) {
    for (int32_t i = 0; i < numFrames; ++i) {
        int w = writeIndex_.load(std::memory_order_relaxed);
        int next = (w + 1) % RING_CAPACITY;
        if (next == readIndex_.load(std::memory_order_acquire)) {
            break;  // Buffer full, drop
        }
        ringBuffer_[w] = data[i];
        writeIndex_.store(next, std::memory_order_release);
    }
}

void OboeAudioEngine::pullFrames(int16_t* data, int32_t numFrames) {
    for (int32_t i = 0; i < numFrames; ++i) {
        int r = readIndex_.load(std::memory_order_relaxed);
        if (r == writeIndex_.load(std::memory_order_acquire)) {
            data[i] = 0;  // Buffer empty, output silence
        } else {
            data[i] = ringBuffer_[r];
            readIndex_.store((r + 1) % RING_CAPACITY, std::memory_order_release);
        }
    }
}

bool OboeAudioEngine::start() {
    if (running_.load()) return true;

    oboe::Result result;

    result = createInputBuilder().openStream(inputStream_);
    if (result != oboe::Result::OK || !inputStream_) {
        LOGI("Failed to open input: %s", oboe::convertToText(result));
        return false;
    }

    routedInputDeviceId_.store(inputStream_->getDeviceId(), std::memory_order_release);

    int32_t inputRate = inputStream_->getSampleRate();
    LOGI("Input: %d Hz", inputRate);
    {
        std::lock_guard<std::mutex> lock(paramMutex_);
        processor_.setSampleRate(inputRate);
    }

    auto outBuilder = createOutputBuilder();
    outBuilder.setSampleRate(inputRate);
    result = outBuilder.openStream(outputStream_);
    if (result != oboe::Result::OK || !outputStream_) {
        LOGI("Failed to open output: %s", oboe::convertToText(result));
        inputStream_->close();
        inputStream_.reset();
        return false;
    }

    result = outputStream_->requestStart();
    if (result != oboe::Result::OK) {
        LOGI("Failed to start output: %s", oboe::convertToText(result));
        outputStream_->close();
        inputStream_->close();
        return false;
    }

    result = inputStream_->requestStart();
    if (result != oboe::Result::OK) {
        LOGI("Failed to start input: %s", oboe::convertToText(result));
        outputStream_->requestStop();
        outputStream_->close();
        inputStream_->close();
        return false;
    }

    writeIndex_.store(0);
    readIndex_.store(0);
    running_.store(true);

    inputThread_ = std::thread([this]() {
        int16_t buffer[kBufferFrames];
        while (running_.load() && inputStream_) {
            auto r = inputStream_->read(buffer, kBufferFrames, kReadTimeoutNs);
            if (r && r.value() > 0) {
                processInputFrames(buffer, r.value());
            } else if (r.error() == oboe::Result::ErrorDisconnected) {
                break;
            }
        }
    });

    return true;
}

void OboeAudioEngine::stop() {
    running_.store(false);

    if (inputThread_.joinable()) {
        inputThread_.join();
    }

    if (inputStream_) {
        inputStream_->requestStop();
        inputStream_->close();
        inputStream_.reset();
    }
    if (outputStream_) {
        outputStream_->requestStop();
        outputStream_->close();
        outputStream_.reset();
    }

    inputLevel_.store(0.0f);
    routedInputDeviceId_.store(0, std::memory_order_release);
}

oboe::DataCallbackResult OboeAudioEngine::onAudioReady(
    oboe::AudioStream* stream,
    void* audioData,
    int32_t numFrames) {

    (void)stream;
    pullFrames(static_cast<int16_t*>(audioData), numFrames);
    return oboe::DataCallbackResult::Continue;
}

void OboeAudioEngine::onErrorAfterClose(oboe::AudioStream* stream, oboe::Result error) {
    (void)stream;
    LOGI("Stream error: %s", oboe::convertToText(error));
    running_.store(false);
}

void OboeAudioEngine::getVisualizationData(int16_t* outBuffer, int32_t maxFrames) {
    int count = std::min(maxFrames, VIS_BUFFER_SIZE);
    int startIdx = (visWriteIndex_.load(std::memory_order_acquire) - count + VIS_BUFFER_SIZE) % VIS_BUFFER_SIZE;
    for (int i = 0; i < count; ++i) {
        outBuffer[i] = visBuffer_[(startIdx + i) % VIS_BUFFER_SIZE];
    }
}

void OboeAudioEngine::setPreferredInputDeviceId(int32_t deviceId) {
    preferredInputDeviceId_.store(deviceId, std::memory_order_release);
    // Applying a new input device requires reopening the input stream; we restart if running.
    if (running_.load()) {
        stop();
        start();
    }
}
