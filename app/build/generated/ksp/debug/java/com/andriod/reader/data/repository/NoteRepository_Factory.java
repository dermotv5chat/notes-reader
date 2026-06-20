package com.andriod.reader.data.repository;

import com.andriod.reader.data.local.NoteFileStore;
import com.andriod.reader.data.local.SyncStateStore;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class NoteRepository_Factory implements Factory<NoteRepository> {
  private final Provider<NoteFileStore> noteFileStoreProvider;

  private final Provider<SyncStateStore> syncStateStoreProvider;

  public NoteRepository_Factory(Provider<NoteFileStore> noteFileStoreProvider,
      Provider<SyncStateStore> syncStateStoreProvider) {
    this.noteFileStoreProvider = noteFileStoreProvider;
    this.syncStateStoreProvider = syncStateStoreProvider;
  }

  @Override
  public NoteRepository get() {
    return newInstance(noteFileStoreProvider.get(), syncStateStoreProvider.get());
  }

  public static NoteRepository_Factory create(Provider<NoteFileStore> noteFileStoreProvider,
      Provider<SyncStateStore> syncStateStoreProvider) {
    return new NoteRepository_Factory(noteFileStoreProvider, syncStateStoreProvider);
  }

  public static NoteRepository newInstance(NoteFileStore noteFileStore,
      SyncStateStore syncStateStore) {
    return new NoteRepository(noteFileStore, syncStateStore);
  }
}
