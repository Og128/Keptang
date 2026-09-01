package com.keptang.capture

import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Writes a standard 44-byte PCM WAV header, used to finalize a file after raw PCM has been appended. */
object WavFileWriter {

    private const val HEADER_SIZE = 44

    fun reserveHeader(file: RandomAccessFile) {
        file.setLength(HEADER_SIZE.toLong())
        file.seek(HEADER_SIZE.toLong())
    }

    fun finalize(file: RandomAccessFile, sampleRateHz: Int, channelCount: Int, bitsPerSample: Int) {
        val dataSize = file.length() - HEADER_SIZE
        val byteRate = sampleRateHz * channelCount * bitsPerSample / 8
        val blockAlign = channelCount * bitsPerSample / 8

        val header = ByteBuffer.allocate(HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray())
        header.putInt((36 + dataSize).toInt())
        header.put("WAVE".toByteArray())
        header.put("fmt ".toByteArray())
        header.putInt(16) // PCM fmt chunk size
        header.putShort(1) // audio format = PCM
        header.putShort(channelCount.toShort())
        header.putInt(sampleRateHz)
        header.putInt(byteRate)
        header.putShort(blockAlign.toShort())
        header.putShort(bitsPerSample.toShort())
        header.put("data".toByteArray())
        header.putInt(dataSize.toInt())

        file.seek(0)
        file.write(header.array())
    }
}
