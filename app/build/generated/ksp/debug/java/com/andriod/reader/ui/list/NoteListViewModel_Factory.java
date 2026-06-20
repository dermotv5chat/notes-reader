package com.andriod.reader.ui.list;

import com.andriod.reader.data.repository.NoteRepository;
import com.andriod.reader.data.repository.SyncRepository;
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
public final class NoteListViewModel_Factory implements Factory<NoteListViewModel> {
  private final Provider<NoteRepository> noteRepositoryProvider;

  private final Provider<SyncRepository> syncRepositoryProvider;

  public NoteListViewModel_Factory(Provider<NoteRepository> noteRepositoryProvider,
      Provider<SyncRepository> syncRepositoryProvider) {
    this.noteRepositoryProvider = noteRepositoryProvider;
    this.syncRepositoryProvider = syncRepositoryProvider;
  }

  @Override
  public NoteListViewModel get() {
    return newInstance(noteRepositoryProvider.get(), syncRepositoryProvider.get());
  }

  public static NoteListViewModel_Factory create(Provider<NoteRepository> noteRepositoryProvider,
      Provider<SyncRepository> syncRepositoryProvider) {
    return new NoteListViewModel_Factory(noteRepositoryProvider, syncRepositoryProvider);
  }

  public static NoteListViewModel newInstance(NoteRepository noteRepository,
      SyncRepository syncRepository) {
    return new NoteListViewModel(noteRepository, syncRepository);
  }
}
