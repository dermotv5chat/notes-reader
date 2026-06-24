package com.andriod.reader.service

import android.content.Context
import com.andriod.reader.data.local.AppDiagnosticLog
import com.andriod.reader.data.remote.SettingsStore
import com.andriod.reader.service.synthesis.EdgeFullTextSynthesizer
import com.andriod.reader.service.synthesis.SherpaFullTextSynthesizer
import com.andriod.reader.service.synthesis.SherpaModelManager
import com.andriod.reader.service.synthesis.TtsPreSynthPipeline
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TtsSynthesisModule {
    @Provides
    @Singleton
    fun provideTtsPreSynthPipeline(
        @ApplicationContext context: Context,
        settingsStore: SettingsStore,
        diagnosticLog: AppDiagnosticLog,
    ): TtsPreSynthPipeline {
        val modelManager = SherpaModelManager(context, diagnosticLog)
        return TtsPreSynthPipeline(
            context = context,
            settingsStore = settingsStore,
            diagnosticLog = diagnosticLog,
            edgeSynthesizer = EdgeFullTextSynthesizer(context, settingsStore, diagnosticLog),
            sherpaSynthesizer = SherpaFullTextSynthesizer(context, modelManager, diagnosticLog),
        )
    }
}
