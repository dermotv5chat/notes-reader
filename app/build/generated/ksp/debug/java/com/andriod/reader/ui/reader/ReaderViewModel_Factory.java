package com.andriod.reader.ui.reader;

import android.content.Context;
import androidx.lifecycle.SavedStateHandle;
import com.andriod.reader.data.remote.SettingsStore;
import com.andriod.reader.data.repository.NoteRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation"
})
public final class ReaderViewModel_Factory implements Factory<ReaderViewModel> {
  private final Provider<Context> contextProvider;

  private final Provider<SavedStateHandle> savedStateHandleProvider;

  private final Provider<NoteRepository> noteRepositoryProvider;

  private final Provider<SettingsStore> settingsStoreProvider;

  public ReaderViewModel_Factory(Provider<Context> contextProvider,
      Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<NoteRepository> noteRepositoryProvider,
      Provider<SettingsStore> settingsStoreProvider) {
    this.contextProvider = contextProvider;
    this.savedStateHandleProvider = savedStateHandleProvider;
    this.noteRepositoryProvider = noteRepositoryProvider;
    this.settingsStoreProvider = settingsStoreProvider;
  }

  @Override
  public ReaderViewModel get() {
    return newInstance(contextProvider.get(), savedStateHandleProvider.get(), noteRepositoryProvider.get(), settingsStoreProvider.get());
  }

  public static ReaderViewModel_Factory create(Provider<Context> contextProvider,
      Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<NoteRepository> noteRepositoryProvider,
      Provider<SettingsStore> settingsStoreProvider) {
    return new ReaderViewModel_Factory(contextProvider, savedStateHandleProvider, noteRepositoryProvider, settingsStoreProvider);
  }

  public static ReaderViewModel newInstance(Context context, SavedStateHandle savedStateHandle,
      NoteRepository noteRepository, SettingsStore settingsStore) {
    return new ReaderViewModel(context, savedStateHandle, noteRepository, settingsStore);
  }
}
