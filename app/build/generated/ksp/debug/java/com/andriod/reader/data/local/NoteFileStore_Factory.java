package com.andriod.reader.data.local;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class NoteFileStore_Factory implements Factory<NoteFileStore> {
  private final Provider<Context> contextProvider;

  private final Provider<SyncStateStore> syncStateStoreProvider;

  private final Provider<TrashStore> trashStoreProvider;

  private final Provider<FolderStore> folderStoreProvider;

  public NoteFileStore_Factory(Provider<Context> contextProvider,
      Provider<SyncStateStore> syncStateStoreProvider, Provider<TrashStore> trashStoreProvider,
      Provider<FolderStore> folderStoreProvider) {
    this.contextProvider = contextProvider;
    this.syncStateStoreProvider = syncStateStoreProvider;
    this.trashStoreProvider = trashStoreProvider;
    this.folderStoreProvider = folderStoreProvider;
  }

  @Override
  public NoteFileStore get() {
    return newInstance(contextProvider.get(), syncStateStoreProvider.get(), trashStoreProvider.get(), folderStoreProvider.get());
  }

  public static NoteFileStore_Factory create(Provider<Context> contextProvider,
      Provider<SyncStateStore> syncStateStoreProvider, Provider<TrashStore> trashStoreProvider,
      Provider<FolderStore> folderStoreProvider) {
    return new NoteFileStore_Factory(contextProvider, syncStateStoreProvider, trashStoreProvider, folderStoreProvider);
  }

  public static NoteFileStore newInstance(Context context, SyncStateStore syncStateStore,
      TrashStore trashStore, FolderStore folderStore) {
    return new NoteFileStore(context, syncStateStore, trashStore, folderStore);
  }
}
