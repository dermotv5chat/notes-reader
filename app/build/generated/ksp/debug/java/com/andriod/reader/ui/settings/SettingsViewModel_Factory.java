package com.andriod.reader.ui.settings;

import android.content.Context;
import com.andriod.reader.data.remote.SettingsStore;
import com.andriod.reader.data.repository.SyncRepository;
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
public final class SettingsViewModel_Factory implements Factory<SettingsViewModel> {
  private final Provider<Context> contextProvider;

  private final Provider<SettingsStore> settingsStoreProvider;

  private final Provider<SyncRepository> syncRepositoryProvider;

  public SettingsViewModel_Factory(Provider<Context> contextProvider,
      Provider<SettingsStore> settingsStoreProvider,
      Provider<SyncRepository> syncRepositoryProvider) {
    this.contextProvider = contextProvider;
    this.settingsStoreProvider = settingsStoreProvider;
    this.syncRepositoryProvider = syncRepositoryProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(contextProvider.get(), settingsStoreProvider.get(), syncRepositoryProvider.get());
  }

  public static SettingsViewModel_Factory create(Provider<Context> contextProvider,
      Provider<SettingsStore> settingsStoreProvider,
      Provider<SyncRepository> syncRepositoryProvider) {
    return new SettingsViewModel_Factory(contextProvider, settingsStoreProvider, syncRepositoryProvider);
  }

  public static SettingsViewModel newInstance(Context context, SettingsStore settingsStore,
      SyncRepository syncRepository) {
    return new SettingsViewModel(context, settingsStore, syncRepository);
  }
}
