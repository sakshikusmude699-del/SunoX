#ifndef SOUNDAMPLIFIER_OBOE_AUDIO_ENGINE_H
#define SOUNDAMPLIFIER_OBOE_AUDIO_ENGINE_H

#include <oboe/Oboe.h>
#include <mutex>
#include <atomic>
#include <thread>
#include "audio_processor.h"

/**
 * Oboe-based low-latency audio engine.
 * Captures from mic, processes via AudioProcessor, outputs to speaker.
 * Target: sub-20ms round-trip latency.
 */
class OboeAudioEngine : public oboe::AudioStreamDataCallback,
                        public oboe::AudioStreamErrorCallback {
public:
    OboeAudioEngine();
    ~OboeAudioEngine();

    bool start();
    void stop();
    bool isRunning() const { return running_.load(); }

    // Mic routing
    void setPreferredInputDeviceId(int32_t deviceId);
    int32_t getPreferredInputDeviceId() const { return preferredInputDeviceId_.load(); }
    int32_t getRoutedInputDeviceId() const { return routedInputDeviceId_.load(); }

    // Parameter updates (thread-safe)
    AudioProcessor& processor() { return processor_; }
    const AudioProcessor& processor() const { return processor_; }
    std::mutex& processorMutex() { return paramMutex_; }

    // Input level for UI (RMS, 0-1)
    float getInputLevel() const { return inputLevel_.load(); }

    // Visualization: last 512 processed output samples
    void getVisualizationData(int16_t* outBuffer, int32_t maxFrames);

private:
    // Output stream callback - we provide processed audio
    oboe::DataCallbackResult onAudioReady(
        oboe::AudioStream* stream,
        void* audioData,
        int32_t numFrames) override;

    // Input stream callback - we receive mic data (via separate stream)
    void processInputFrames(const int16_t* data, int32_t numFrames);

    void onErrorAfterClose(oboe::AudioStream* stream, oboe::Result error) override;

    oboe::AudioStreamBuilder createInputBuilder();
    oboe::AudioStreamBuilder createOutputBuilder();

    std::shared_ptr<oboe::AudioStream> inputStream_;
    std::shared_ptr<oboe::AudioStream> outputStream_;

    AudioProcessor processor_;
    std::atomic<bool> running_{false};
    std::atomic<float> inputLevel_{0.0f};
    std::atomic<int32_t> preferredInputDeviceId_{0};
    std::atomic<int32_t> routedInputDeviceId_{0};

    static constexpr int VIS_BUFFER_SIZE = 512;
    int16_t visBuffer_[VIS_BUFFER_SIZE];
    std::atomic<int> visWriteIndex_{0};

    // Ring buffer: input writes, output reads
    static constexpr int RING_CAPACITY = 4096;
    int16_t ringBuffer_[RING_CAPACITY];
    std::atomic<int> writeIndex_{0};
    std::atomic<int> readIndex_{0};
    std::mutex paramMutex_;

    int getAvailableFrames() const;
    void pushFrames(const int16_t* data, int32_t numFrames);
    void pullFrames(int16_t* data, int32_t numFrames);
    float computeRms(const int16_t* data, int32_t numFrames);

    std::thread inputThread_;
};

#endif // SOUNDAMPLIFIER_OBOE_AUDIO_ENGINE_H
