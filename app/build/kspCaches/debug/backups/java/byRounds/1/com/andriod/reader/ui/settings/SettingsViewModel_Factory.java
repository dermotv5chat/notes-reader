package com.andriod.reader.ui.settings;

import com.andriod.reader.data.remote.SettingsStore;
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
public final class SettingsViewModel_Factory implements Factory<SettingsViewModel> {
  private final Provider<SettingsStore> settingsStoreProvider;

  private final Provider<SyncRepository> syncRepositoryProvider;

  public SettingsViewModel_Factory(Provider<SettingsStore> settingsStoreProvider,
      Provider<SyncRepository> syncRepositoryProvider) {
    this.settingsStoreProvider = settingsStoreProvider;
    this.syncRepositoryProvider = syncRepositoryProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(settingsStoreProvider.get(), syncRepositoryProvider.get());
  }

  public static SettingsViewModel_Factory create(Provider<SettingsStore> settingsStoreProvider,
      Provider<SyncRepository> syncRepositoryProvider) {
    return new SettingsViewModel_Factory(settingsStoreProvider, syncRepositoryProvider);
  }

  public static SettingsViewModel newInstance(SettingsStore settingsStore,
      SyncRepository syncRepository) {
    return new SettingsViewModel(settingsStore, syncRepository);
  }
}
