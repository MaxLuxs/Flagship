package io.maxluxs.flagship.core.evaluator

import io.maxluxs.flagship.core.model.EvalContext
import io.maxluxs.flagship.core.model.ExperimentAssignment
import io.maxluxs.flagship.core.model.ExperimentDefinition
import io.maxluxs.flagship.core.model.Variant
import kotlin.math.absoluteValue

object BucketingEngine {
    fun assign(
        experiment: ExperimentDefinition,
        context: EvalContext
    ): ExperimentAssignment? {
        // Check targeting
        experiment.targeting?.let { targeting ->
            if (!TargetingEvaluator.evaluate(targeting, context)) {
                return null
            }
        }

        // Select variant based on bucketing
        val userId = context.userId ?: context.deviceId ?: return null
        val variant = selectVariant(experiment.key, userId, experiment.variants)

        return ExperimentAssignment(
            key = experiment.key,
            variant = variant.name,
            payload = variant.payload,
            hash = generateHash(experiment.key, userId)
        )
    }

    private fun selectVariant(
        experimentKey: String,
        userId: String,
        variants: List<Variant>
    ): Variant {
        if (variants.isEmpty()) {
            throw IllegalArgumentException("Experiment must have at least one variant")
        }

        val hash = hashForBucketing("$experimentKey:$userId")
        var cumulative = 0.0

        for (variant in variants) {
            cumulative += variant.weight
            if (hash < cumulative) {
                return variant
            }
        }

        return variants.last()
    }

    fun isInBucket(id: String, percent: Int): Boolean {
        require(percent in 0..100) { "Percent must be between 0 and 100" }
        // Use MurmurHash3 for better distribution and platform consistency
        val hash = (murmurHash3(id.encodeToByteArray()) and 0x7FFFFFFF) % 100
        return hash < percent
    }

    private fun hashForBucketing(input: String): Double {
        // Use MurmurHash3 and normalize to 0.0..1.0
        val hash = murmurHash3(input.encodeToByteArray()) and 0x7FFFFFFF
        return (hash % 10000) / 10000.0
    }

    private fun generateHash(experimentKey: String, userId: String): String {
        return murmurHash3("$experimentKey:$userId".encodeToByteArray()).toString(16)
    }

    /**
     * MurmurHash3 x86 32-bit implementation
     */
    private fun murmurHash3(data: ByteArray, seed: Int = 0): Int {
        val c1 = 0xcc9e2d51.toInt()
        val c2 = 0x1b873593.toInt()
        var h1 = seed
        val length = data.size
        var i = 0

        while (i + 4 <= length) {
            var k1 = (data[i].toInt() and 0xFF) or
                    ((data[i + 1].toInt() and 0xFF) shl 8) or
                    ((data[i + 2].toInt() and 0xFF) shl 16) or
                    ((data[i + 3].toInt() and 0xFF) shl 24)

            k1 *= c1
            k1 = (k1 shl 15) or (k1 ushr 17)
            k1 *= c2

            h1 = h1 xor k1
            h1 = (h1 shl 13) or (h1 ushr 19)
            h1 = h1 * 5 + 0xe6546b64.toInt()

            i += 4
        }

        var k1 = 0
        val remaining = length - i
        if (remaining == 3) k1 = k1 or ((data[i + 2].toInt() and 0xFF) shl 16)
        if (remaining >= 2) k1 = k1 or ((data[i + 1].toInt() and 0xFF) shl 8)
        if (remaining >= 1) {
            k1 = k1 or (data[i].toInt() and 0xFF)
            k1 *= c1
            k1 = (k1 shl 15) or (k1 ushr 17)
            k1 *= c2
            h1 = h1 xor k1
        }

        h1 = h1 xor length
        h1 = fmix32(h1)
        return h1
    }

    private fun fmix32(h: Int): Int {
        var h1 = h
        h1 = h1 xor (h1 ushr 16)
        h1 *= 0x85ebca6b.toInt()
        h1 = h1 xor (h1 ushr 13)
        h1 *= 0xc2b2ae35.toInt()
        h1 = h1 xor (h1 ushr 16)
        return h1
    }
}

