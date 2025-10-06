package no.nav.hag.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import java.util.UUID

class ForespoerselListeTest {

    val uuid1 = UUID.randomUUID().toString()
    val uuid2 = UUID.randomUUID().toString()


    @Test
    fun `skal konvertere gyldige UUIDer`() {

        val input = uuid1+";"+uuid2
        val forespoerselListe = ForespoerselListe(input)

        val expected = mapOf(
            uuid1 to UUID.fromString(uuid1),
            uuid2 to UUID.fromString(uuid2),
        )

        assertEquals(expected, forespoerselListe.konverterInput())
    }

    @Test
    fun `skal trimme bort blanke tegn og felter`() {

        val input = "   ;" +uuid1+";   ;"+uuid2+";  "
        val forespoerselListe = ForespoerselListe(input)

        val expected = mapOf(
            uuid1 to UUID.fromString(uuid1),
            uuid2 to UUID.fromString(uuid2),
        )

        assertEquals(expected, forespoerselListe.konverterInput())
    }

    @Test
    fun `skal trimme bort newlines`() {

        val input = "   " +uuid1+"\n;"+uuid2+"  ;\n  "
        val forespoerselListe = ForespoerselListe(input)

        val expected = mapOf(
            uuid1 to UUID.fromString(uuid1),
            uuid2 to UUID.fromString(uuid2),
        )

        assertEquals(expected, forespoerselListe.konverterInput())

        val formattertInput = """
            123e4567-e89b-12d3-a456-426614174000;
            123e4567-e89b-12d3-a456-426614174001;
            123e4567-e89b-12d3-a456-426614174002;
        """.trimIndent()

        val forespoerselListe2 = ForespoerselListe(formattertInput)

        val expected2 = mapOf(
            "123e4567-e89b-12d3-a456-426614174000" to UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
            "123e4567-e89b-12d3-a456-426614174001" to UUID.fromString("123e4567-e89b-12d3-a456-426614174001"),
            "123e4567-e89b-12d3-a456-426614174002" to UUID.fromString("123e4567-e89b-12d3-a456-426614174002"),
        )

        assertEquals(expected2, forespoerselListe2.konverterInput())
    }

    @Test
    fun `tom input gir tomt map`() {
        val input = ""
        val forespoerselListe = ForespoerselListe(input)

        val expected = emptyMap<String, UUID?>()

        assertEquals(expected, forespoerselListe.konverterInput())
    }

    @Test
    fun `skal håndtere ugyldige uuid`() {
        val input = "invalid1;invalid2"
        val forespoerselListe = ForespoerselListe(input)

        val expected = mapOf(
            "invalid1" to null,
            "invalid2" to null
        )

        assertEquals(expected, forespoerselListe.konverterInput())
    }

    @Test
    fun `skal håndtere blanding av gyldige og ugyldige uuid`() {
        val input = uuid1 + ";invalid;" + uuid2
        val forespoerselListe = ForespoerselListe(input)

        val expected = mapOf(
            uuid1 to UUID.fromString(uuid1),
            "invalid" to null,
            uuid2 to UUID.fromString(uuid2),
        )

        assertEquals(expected, forespoerselListe.konverterInput())
    }
}
