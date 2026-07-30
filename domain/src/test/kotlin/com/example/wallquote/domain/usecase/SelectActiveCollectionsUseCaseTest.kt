package com.example.wallquote.domain.usecase

import com.example.wallquote.domain.model.BackgroundSpec
import com.example.wallquote.domain.model.CollectionConfig
import com.example.wallquote.domain.model.DailyTimeRange
import com.example.wallquote.domain.model.QuoteLine
import com.example.wallquote.domain.model.TextStyleConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class SelectActiveCollectionsUseCaseTest {

    private val useCase = SelectActiveCollectionsUseCase()

    private fun collection(
        id: Long,
        start: Int,
        end: Int,
    ) = CollectionConfig(
        id = id,
        name = "c$id",
        schedule = DailyTimeRange(start, end),
        background = BackgroundSpec.Solid("#000000"),
        lines = listOf(QuoteLine(id = 1, text = "t", displayOrder = 0)),
        textStyle = TextStyleConfig(),
    )

    @Test
    fun filtersBySchedule() {
        val allDay = collection(1, 0, 0)
        val morning = collection(2, 6 * 60, 12 * 60)
        val list = listOf(allDay, morning)
        val at9am = 9 * 60
        val active = useCase(list, at9am)
        assertEquals(listOf(allDay, morning), active)
        val at3pm = 15 * 60
        assertEquals(listOf(allDay), useCase(list, at3pm))
    }
}
