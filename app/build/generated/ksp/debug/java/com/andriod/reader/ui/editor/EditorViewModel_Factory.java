package com.andriod.reader.ui.editor;

import androidx.lifecycle.SavedStateHandle;
import com.andriod.reader.data.repository.NoteRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
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
public final class EditorViewModel_Factory implements Factory<EditorViewModel> {
  private final Provider<SavedStateHandle> savedStateHandleProvider;

  private final Provider<NoteRepository> noteRepositoryProvider;

  public EditorViewModel_Factory(Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<NoteRepository> noteRepositoryProvider) {
    this.savedStateHandleProvider = savedStateHandleProvider;
    this.noteRepositoryProvider = noteRepositoryProvider;
  }

  @Override
  public EditorViewModel get() {
    return newInstance(savedStateHandleProvider.get(), noteRepositoryProvider.get());
  }

  public static EditorViewModel_Factory create(Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<NoteRepository> noteRepositoryProvider) {
    return new EditorViewModel_Factory(savedStateHandleProvider, noteRepositoryProvider);
  }

  public static EditorViewModel newInstance(SavedStateHandle savedStateHandle,
      NoteRepository noteRepository) {
    return new EditorViewModel(savedStateHandle, noteRepository);
  }
}
