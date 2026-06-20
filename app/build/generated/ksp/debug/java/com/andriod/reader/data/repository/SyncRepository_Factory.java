package com.andriod.reader.data.repository;

import com.andriod.reader.data.local.NoteFileStore;
import com.andriod.reader.data.local.SyncStateStore;
import com.andriod.reader.data.remote.GitHubApi;
import com.andriod.reader.data.remote.SettingsStore;
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
public final class SyncRepository_Factory implements Factory<SyncRepository> {
  private final Provider<GitHubApi> gitHubApiProvider;

  private final Provider<SettingsStore> settingsStoreProvider;

  private final Provider<NoteFileStore> noteFileStoreProvider;

  private final Provider<SyncStateStore> syncStateStoreProvider;

  public SyncRepository_Factory(Provider<GitHubApi> gitHubApiProvider,
      Provider<SettingsStore> settingsStoreProvider, Provider<NoteFileStore> noteFileStoreProvider,
      Provider<SyncStateStore> syncStateStoreProvider) {
    this.gitHubApiProvider = gitHubApiProvider;
    this.settingsStoreProvider = settingsStoreProvider;
    this.noteFileStoreProvider = noteFileStoreProvider;
    this.syncStateStoreProvider = syncStateStoreProvider;
  }

  @Override
  public SyncRepository get() {
    return newInstance(gitHubApiProvider.get(), settingsStoreProvider.get(), noteFileStoreProvider.get(), syncStateStoreProvider.get());
  }

  public static SyncRepository_Factory create(Provider<GitHubApi> gitHubApiProvider,
      Provider<SettingsStore> settingsStoreProvider, Provider<NoteFileStore> noteFileStoreProvider,
      Provider<SyncStateStore> syncStateStoreProvider) {
    return new SyncRepository_Factory(gitHubApiProvider, settingsStoreProvider, noteFileStoreProvider, syncStateStoreProvider);
  }

  public static SyncRepository newInstance(GitHubApi gitHubApi, SettingsStore settingsStore,
      NoteFileStore noteFileStore, SyncStateStore syncStateStore) {
    return new SyncRepository(gitHubApi, settingsStore, noteFileStore, syncStateStore);
  }
}
