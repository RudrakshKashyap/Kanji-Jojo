package ua.syt0r.kanji.presentation.screen.main.screen.practice_letter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ua.syt0r.kanji.core.japanese.KanaReading
import ua.syt0r.kanji.presentation.common.textDp
import ua.syt0r.kanji.presentation.common.ui.kanji.KanjiReadingsContainer
import ua.syt0r.kanji.presentation.screen.main.screen.practice_letter.data.LetterPracticeItemData
import ua.syt0r.kanji.presentation.screen.main.screen.practice_letter.data.LetterPracticeReviewState

@Composable
fun LetterPracticeReadingInfoSection(
    state: LetterPracticeReviewState.Reading,
    speakKana: (KanaReading) -> Unit,
    modifier: Modifier,
    notesContent: @Composable (String) -> Unit = {}
) {

    val revealed = rememberUpdatedState(state.revealed.value)
    val alpha = remember { derivedStateOf { if (revealed.value) 1f else 0f } }
    val clickable by remember { derivedStateOf { alpha.value == 1f } }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        Text(
            text = state.itemData.character,
            fontSize = 80.textDp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        when (state.itemData) {

            is LetterPracticeItemData.KanaReadingData -> {
                KanaDetails(
                    data = state.itemData,
                    kanaAutoPlay = state.layout.kanaAutoPlay,
                    toggleKanaAutoPlay = {
                        state.layout.run { kanaAutoPlay.value = !kanaAutoPlay.value }
                    },
                    speakKana = speakKana,
                    alpha = alpha,
                    clickable = clickable
                )
            }

            is LetterPracticeItemData.KanjiReadingData -> {
                KanjiDetails(
                    data = state.itemData,
                    alpha = alpha
                )
            }

        }

        notesContent(state.itemData.character)

    }

}

@Composable
private fun ColumnScope.KanaDetails(
    data: LetterPracticeItemData.KanaReadingData,
    kanaAutoPlay: State<Boolean>,
    toggleKanaAutoPlay: () -> Unit,
    speakKana: (reading: KanaReading) -> Unit,
    alpha: State<Float>,
    clickable: Boolean
) {

    LetterPracticeKanaInfo(
        kanaSystem = data.kanaSystem,
        reading = data.reading,
        modifier = Modifier.align(Alignment.CenterHorizontally).alpha(alpha.value)
    )

    KanaVoiceMenu(
        autoPlayEnabled = kanaAutoPlay,
        clickable = clickable,
        onAutoPlayToggleClick = toggleKanaAutoPlay,
        onSpeakClick = { speakKana(data.reading) },
        modifier = Modifier.align(Alignment.CenterHorizontally).alpha(alpha.value)
    )

}

@Composable
private fun ColumnScope.KanjiDetails(
    data: LetterPracticeItemData.KanjiReadingData,
    alpha: State<Float>
) {

    if (data.meanings.isNotEmpty()) {
        Text(
            text = data.meanings.joinToString(),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.CenterHorizontally).alpha(alpha.value)
        )
    }

    KanjiReadingsContainer(
        on = data.on,
        kun = data.kun,
        modifier = Modifier.fillMaxWidth().alpha(alpha.value)
    )

    if (data.variants != null) {
        KanjiVariantsRow(
            variants = data.variants,
            modifier = Modifier.alpha(alpha.value)
        )
    }

}
