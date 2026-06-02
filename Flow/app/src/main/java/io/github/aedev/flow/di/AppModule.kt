package io.github.aedev.flow.di

import android.content.Context
import io.github.aedev.flow.BuildConfig
import io.github.aedev.flow.innertube.YouTube
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.crossfade
import coil3.util.DebugLogger
import okhttp3.OkHttpClient
import okio.Path.Companion.toPath
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context {
        return context
    }

    @Provides
    @Singleton
    fun provideYouTube(): YouTube {
        return YouTube
    }

    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient
    ): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(coil3.network.okhttp.OkHttpNetworkFetcherFactory(okHttpClient))
            }
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache").absolutePath.toPath())
                    .maxSizePercent(0.02)
                    .build()
            }
            .crossfade(true)
            .apply { if (BuildConfig.DEBUG) logger(DebugLogger()) }
            .build()
    }
}
