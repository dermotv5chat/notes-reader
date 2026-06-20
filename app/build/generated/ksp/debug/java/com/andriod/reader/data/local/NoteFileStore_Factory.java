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

  public NoteFileStore_Factory(Provider<Context> contextProvider,
      Provider<SyncStateStore> syncStateStoreProvider, Provider<TrashStore> trashStoreProvider) {
    this.contextProvider = contextProvider;
    this.syncStateStoreProvider = syncStateStoreProvider;
    this.trashStoreProvider = trashStoreProvider;
  }

  @Override
  public NoteFileStore get() {
    return newInstance(contextProvider.get(), syncStateStoreProvider.get(), trashStoreProvider.get());
  }

  public static NoteFileStore_Factory create(Provider<Context> contextProvider,
      Provider<SyncStateStore> syncStateStoreProvider, Provider<TrashStore> trashStoreProvider) {
    return new NoteFileStore_Factory(contextProvider, syncStateStoreProvider, trashStoreProvider);
  }

  public static NoteFileStore newInstance(Context context, SyncStateStore syncStateStore,
      TrashStore trashStore) {
    return new NoteFileStore(context, syncStateStore, trashStore);
  }
}
