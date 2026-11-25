package com.zobaer53.plantapp.feature.diagnosis.navigation

import com.zobaer53.plantapp.core.domain.model.LeafDisease
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Plan risk: DiseaseInfoRoute carries an enum from another module. Navigation 3 saves keys with
 * their generated serializers, so check those serializers round-trip every LeafDisease (through a
 * minimal flat encoder; the Bundle-backed one needs a device).
 */
@OptIn(ExperimentalSerializationApi::class)
class DiagnosisRouteSerializationTest {

    @Test
    fun diseaseInfoRoute_encodesDiseaseAsEnum() {
        val descriptor = DiseaseInfoRoute.serializer().descriptor
        assertEquals(1, descriptor.elementsCount)
        assertEquals("disease", descriptor.getElementName(0))
        assertEquals(SerialKind.ENUM, descriptor.getElementDescriptor(0).kind)
    }

    @Test
    fun diseaseInfoRoute_roundTripsEveryDisease() {
        LeafDisease.entries.forEach { disease ->
            val route = DiseaseInfoRoute(disease)
            assertEquals(route, roundTrip(DiseaseInfoRoute.serializer(), DiseaseInfoRoute.serializer(), route))
        }
    }

    @Test
    fun diagnosisRoute_roundTrips() {
        assertEquals(
            DiagnosisRoute,
            roundTrip(DiagnosisRoute.serializer(), DiagnosisRoute.serializer(), DiagnosisRoute),
        )
    }

    private fun <T> roundTrip(save: SerializationStrategy<T>, restore: DeserializationStrategy<T>, value: T): T {
        val encoder = ListEncoder()
        encoder.encodeSerializableValue(save, value)
        return ListDecoder(ArrayDeque(encoder.values)).decodeSerializableValue(restore)
    }

    private class ListEncoder : AbstractEncoder() {
        val values = mutableListOf<Any>()
        override val serializersModule: SerializersModule = EmptySerializersModule()
        override fun encodeValue(value: Any) {
            values += value
        }
        override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
            values += enumDescriptor.getElementName(index)
        }
    }

    private class ListDecoder(private val values: ArrayDeque<Any>) : AbstractDecoder() {
        private var elementIndex = 0
        override val serializersModule: SerializersModule = EmptySerializersModule()
        override fun decodeValue(): Any = values.removeFirst()
        override fun decodeEnum(enumDescriptor: SerialDescriptor): Int =
            enumDescriptor.getElementIndex(values.removeFirst() as String)
        override fun decodeSequentially(): Boolean = true
        override fun decodeElementIndex(descriptor: SerialDescriptor): Int =
            if (elementIndex == descriptor.elementsCount) CompositeDecoder.DECODE_DONE else elementIndex++
    }
}
