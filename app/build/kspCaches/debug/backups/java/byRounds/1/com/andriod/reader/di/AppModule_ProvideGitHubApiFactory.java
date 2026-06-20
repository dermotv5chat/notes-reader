package com.andriod.reader.di;

import com.andriod.reader.data.remote.GitHubApi;
import com.google.gson.Gson;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import okhttp3.OkHttpClient;

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
public final class AppModule_ProvideGitHubApiFactory implements Factory<GitHubApi> {
  private final Provider<OkHttpClient> clientProvider;

  private final Provider<Gson> gsonProvider;

  public AppModule_ProvideGitHubApiFactory(Provider<OkHttpClient> clientProvider,
      Provider<Gson> gsonProvider) {
    this.clientProvider = clientProvider;
    this.gsonProvider = gsonProvider;
  }

  @Override
  public GitHubApi get() {
    return provideGitHubApi(clientProvider.get(), gsonProvider.get());
  }

  public static AppModule_ProvideGitHubApiFactory create(Provider<OkHttpClient> clientProvider,
      Provider<Gson> gsonProvider) {
    return new AppModule_ProvideGitHubApiFactory(clientProvider, gsonProvider);
  }

  public static GitHubApi provideGitHubApi(OkHttpClient client, Gson gson) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideGitHubApi(client, gson));
  }
}
