# Games Tab Documentation

## Overview

The Games tab in CloudStream3 provides users with access to a curated collection of web-based games. The system features intelligent caching, offline support, and a modern UI with search functionality and featured game highlights.

## File Locations

### Core Implementation Files
- **Main Fragment**: `app/src/main/java/com/lagradost/cloudstream3/ui/game/GameFragment.kt`
- **ViewModel**: `app/src/main/java/com/lagradost/cloudstream3/ui/game/GameViewModel.kt`
- **Data Models**: `app/src/main/java/com/lagradost/cloudstream3/ui/game/GameModel.kt`
- **Cache Manager**: `app/src/main/java/com/lagradost/cloudstream3/ui/game/GameCacheManager.kt`
- **Game Player**: `app/src/main/java/com/lagradost/cloudstream3/ui/game/GamePlayerFragment.kt`
- **Adapter**: `app/src/main/java/com/lagradost/cloudstream3/ui/game/GameAdapter.kt`

### Layout Files
- **Main Layout**: `app/src/main/res/layout/fragment_game.xml`
- **Game Player**: `app/src/main/res/layout/fragment_game_player.xml`
- **Game Items**: `app/src/main/res/layout/item_game_normal.xml`, `app/src/main/res/layout/item_game_large.xml`
- **Shimmer**: `app/src/main/res/layout/shimmer_game_grid.xml`

### Resources
- **Icons**: `app/src/main/res/drawable/ic_game_selector.xml`, `app/src/main/res/drawable/ic_game_placeholder.xml`

### External Data
- **Games JSON**: `https://raw.githubusercontent.com/am-abdulmueed/PluginStream-Games/main/games_final_lite.json`
- **GitHub API**: `https://api.github.com/repos/am-abdulmueed/PluginStream-Games/commits`

## Data Models

### GameResponse
```kotlin
data class GameResponse(
    val title: String,
    val total_count: Int,
    val hits: List<GameModel>
)
```

### GameModel
```kotlin
data class GameModel(
    val title: String,
    val gameURL: String,
    val genres: List<String> = emptyList(),
    val images: GameImages,
    val isFeatured: Boolean = false // For large poster display
)
```

### GameImages
```kotlin
data class GameImages(
    val icon: String,
    val poster: String
)
```

## UI Components

### Main Game Screen (`GameFragment`)
- **Search Bar**: Real-time game filtering by title and genres
- **Grid Layout**: Responsive grid with featured games (every 5th game)
- **Shimmer Loading**: Skeleton loading animation for better UX
- **Offline Support**: Shows cached games when offline
- **Navigation**: Direct link to Offers section

### Game Player (`GamePlayerFragment`)
- **WebView Integration**: Embedded web browser for game playback
- **Fullscreen Support**: Immersive gaming experience
- **Network Detection**: Handles online/offline states
- **Progress Indicator**: Loading states for game initialization

### Layout Features
- **Featured Games**: Large poster cards every 5th game (Premium pattern)
- **Normal Games**: Compact card layout with icon and title
- **Search Functionality**: Instant filtering with debounced input
- **Empty States**: Proper handling for no results and offline scenarios

## API Integration

### GitHub-Based Data Source
- **Primary URL**: `https://raw.githubusercontent.com/am-abdulmueed/PluginStream-Games/main/games_final_lite.json`
- **Version Control**: GitHub API for commit hash checking
- **Update Detection**: Smart cache invalidation based on file changes

### Cache Management System
The `GameCacheManager` implements intelligent caching:

#### Key Features
- **Local Storage**: Games cached in SharedPreferences
- **Update Detection**: GitHub commit hash comparison
- **Offline Support**: Works completely offline with cached data
- **Auto-Refresh**: Updates only when GitHub file changes

#### Cache Keys
- `KEY_CACHED_GAMES`: Main games JSON data
- `KEY_LAST_COMMIT`: Last known GitHub commit hash
- `KEY_CACHE_TIMESTAMP`: Cache creation time

#### Cache Logic Flow
1. **First Launch**: Download from GitHub and cache locally
2. **Subsequent Launches**: Check GitHub for updates
3. **If Updated**: Download new data and update cache
4. **If No Updates**: Use cached data immediately
5. **Offline Mode**: Use cached data regardless of updates

## Architecture Pattern

### MVVM Implementation
- **View**: `GameFragment` handles UI and user interactions
- **ViewModel**: `GameViewModel` manages state and business logic
- **Repository**: `GameCacheManager` handles data persistence and API calls

### State Management
- **LiveData**: Reactive UI updates for game list, loading states, and errors
- **Coroutines**: Asynchronous operations for network calls and caching
- **Single Source of Truth**: ViewModel maintains the canonical game list

### Search Implementation
```kotlin
fun filterGames(query: String): List<GameModel> {
    val currentGames = _allGames.value ?: emptyList()
    if (query.isBlank()) return currentGames
    
    return currentGames.filter { game ->
        game.title.contains(query, ignoreCase = true) ||
        game.genres.any { it.contains(query, ignoreCase = true) }
    }
}
```

## API Flow

### Initial Load
1. `GameFragment` → `GameViewModel.fetchGamesIfNeeded()`
2. `GameViewModel` → `GameCacheManager.fetchGamesWithCache()`
3. `GameCacheManager` → Check cache validity
4. If needed: Download from GitHub → Cache locally → Return data
5. If not needed: Return cached data immediately

### Search Flow
1. User types in search bar
2. `GameFragment` → `GameViewModel.filterGames(query)`
3. `GameViewModel` → Filter cached games locally
4. Update UI with filtered results instantly

### Game Launch Flow
1. User clicks on game
2. Navigate to `GamePlayerFragment` with game URL and title
3. `GamePlayerFragment` → Initialize WebView with game URL
4. Load game in fullscreen mode

## Caching Strategy

### Smart Cache Features
- **GitHub Integration**: Automatic update detection via commit hashes
- **Offline First**: Complete offline functionality with cached data
- **Efficient Updates**: Only downloads when file actually changes
- **Fallback Support**: Graceful degradation when network fails

### Cache Performance
- **Instant Loading**: Cached data loads immediately
- **Background Updates**: Checks for updates in background
- **Memory Efficient**: Uses SharedPreferences for persistence
- **Network Optimized**: Minimizes unnecessary API calls

### Cache Management Methods
```kotlin
// Clear cache (force refresh)
fun clearCache(context: Context)

// Check if cache exists
fun hasCache(context: Context): Boolean

// Get cache information
fun getCacheInfo(context: Context): CacheInfo
```

## Error Handling

### Network Errors
- **Graceful Fallback**: Use cached data when network fails
- **User Feedback**: Show appropriate error messages
- **Retry Logic**: Automatic retry with exponential backoff

### UI Error States
- **Loading States**: Shimmer animations during data fetch
- **Empty States**: Proper messaging for no games found
- **Offline Mode**: Clear indication when working offline

### Error Recovery
- **Cache Clear**: Manual cache clearing option for troubleshooting
- **Network Detection**: Automatic offline mode detection
- **Fallback Content**: Always show cached games when available

## Performance Optimizations

### UI Performance
- **RecyclerView**: Efficient list rendering with view recycling
- **Image Loading**: Optimized image caching and loading
- **Shimmer Effects**: Smooth loading animations
- **Lazy Loading**: Games loaded on demand

### Network Performance
- **Smart Caching**: Minimizes network requests
- **Compression**: Efficient JSON data transfer
- **Background Updates**: Non-blocking cache updates
- **Connection Reuse**: HTTP client optimization

### Memory Management
- **ViewModel Lifecycle**: Proper memory cleanup
- **WebView Management**: Proper resource cleanup in game player
- **Image Optimization**: Efficient memory usage for game thumbnails

## Debug Features

### Logging
- **Cache Operations**: Detailed logging for cache hits/misses
- **Network Calls**: Request/response logging for debugging
- **Error Tracking**: Comprehensive error logging

### Cache Debugging
```kotlin
// Get cache info for debugging
val cacheInfo = GameCacheManager.getCacheInfo(context)
Log.d("Games", "Cache exists: ${cacheInfo.hasCache}")
Log.d("Games", "Last update: ${cacheInfo.lastUpdateTimestamp}")
Log.d("Games", "Commit hash: ${cacheInfo.lastCommitHash}")
```

## Customization

### Featured Games Pattern
- **Premium Layout**: Every 5th game gets large poster treatment
- **Automatic Detection**: `isFeatured = (index + 1) % 5 == 0`
- **Visual Hierarchy**: Featured games stand out in the grid

### Search Customization
- **Multi-field Search**: Searches both titles and genres
- **Case Insensitive**: User-friendly search experience
- **Instant Results**: Real-time filtering as user types

### UI Theming
- **Theme Adaptive**: Uses app theme colors and styles
- **Responsive Design**: Works across different screen sizes
- **Material Design**: Follows Material Design 3 guidelines

## Security Considerations

### WebView Security
- **HTTPS Only**: Games loaded over secure connections
- **Sandbox Mode**: WebView runs in secure sandbox
- **Content Security**: Proper content security policies

### Data Security
- **Local Storage**: Games cached in secure SharedPreferences
- **Network Security**: SSL/TLS for all network communications
- **Input Validation**: Proper validation of game URLs and data

## Analytics and Monitoring

### Usage Tracking
- **Game Launches**: Track which games are played most
- **Search Queries**: Analyze user search patterns
- **Cache Performance**: Monitor cache hit rates

### Performance Metrics
- **Load Times**: Track game loading performance
- **Network Usage**: Monitor data transfer efficiency
- **Error Rates**: Track and analyze error occurrences

## Future Enhancements

### Planned Features
- **Game Categories**: Advanced filtering by game genres
- **Favorites System**: User favorite games functionality
- **Recent Games**: Track recently played games
- **Game Statistics**: Play time and achievement tracking

### Technical Improvements
- **Advanced Caching**: More sophisticated cache strategies
- **Offline Sync**: Background synchronization when online
- **Performance Monitoring**: Real-time performance metrics
- **A/B Testing**: Feature experimentation framework

## API Response Example

### Games JSON Structure
```json
{
    "title": "Optimized Games List",
    "total_count": 1493,
    "hits": [
        {
            "title": "Fruits into Baskets",
            "gameURL": "https://playgama.com/export/game/fruits-into-baskets?clid=p_db0585d2-2da5-4641-b768-657993e9122d",
            "genres": ["puzzle", "food", "logic", "easy"],
            "images": {
                "icon": "https://playgama.com/cdn-cgi/imagedelivery/LN2S-4p3-GgZvEx3IPaKUA/9071a445-5cd8-4965-3bba-3fb1ff08d900/enlarged",
                "poster": "https://playgama.com/cdn-cgi/imagedelivery/LN2S-4p3-GgZvEx3IPaKUA/537e972d-b7f4-449a-2795-0c63e717a200/enlarged"
            }
        }
    ]
}
```

### GitHub API Response
```json
[
    {
        "sha": "abc123def456",
        "commit": {
            "message": "Update games list",
            "author": {
                "date": "2026-05-08T12:00:00Z"
            }
        }
    }
]
```

# Playgama Setup Documentation

This document explains how the game list is optimized using `.ps1` (PowerShell) and `.bat` (Batch) scripts.

## Game List Optimization Process

To ensure an efficient and up-to-date game list, the following scripts are utilized:

### 1. PowerShell Scripts (.ps1)

PowerShell scripts are used for more advanced and flexible automation tasks. These scripts typically handle:
- **Dynamic Game Detection:** Scanning specified directories for new game installations or updates.
- **Metadata Extraction:** Extracting game names, executable paths, and other relevant information from game files or configuration files.
- **API Integration:** Potentially interacting with game APIs (if applicable) to fetch additional details, artwork, or update information.
- **Data Transformation:** Processing and formatting the collected game data into a structured format suitable for the game launcher/application.
- **Error Handling and Logging:** Providing robust error handling and logging mechanisms to track issues during the optimization process.

### 2. Batch Scripts (.bat)

Batch scripts are used for simpler, sequential command execution and often act as wrappers or initiators for PowerShell scripts or other executables. Their typical uses include:
- **Initiating Optimization:** Launching the main PowerShell script or a sequence of scripts to start the optimization process.
- **Environment Setup:** Setting up necessary environment variables or checking for prerequisites before running more complex scripts.
- **Scheduled Tasks:** Being scheduled to run at specific intervals (e.g., daily, weekly) to keep the game list optimized automatically.
- **Basic File Operations:** Performing simple file management tasks like moving, copying, or deleting temporary files generated during the process.

## How to Use

To optimize the game list:
1. Navigate to the directory containing the `.ps1` and `.bat` scripts.
2. Run the primary `.bat` script (e.g., `optimize_games.bat`) or the main `.ps1` script (e.g., `UpdateGameList.ps1`) directly.
3. The scripts will automatically detect, process, and update the game list.
4. Review any logs generated by the scripts for successful completion or error messages.

This setup ensures that your game library remains organized and ready for use with minimal manual intervention.

## Games Lite JSON Creation Process

The `games_final_lite.json` file is created through an optimization process that transforms raw game data into a lightweight, app-ready format.

### Raw Data Sources
- **Playgama API**: Primary source for game metadata
- **Game Directories**: Local game installation directories
- **Configuration Files**: Game settings and metadata files
- **Image Assets**: Game icons and poster images

### Optimization Steps

#### 1. Data Collection
```powershell
# Example PowerShell snippet for data collection
$games = Get-ChildItem -Path $gameDirectory -Recurse -Filter "*.exe"
foreach ($game in $games) {
    $metadata = Extract-GameMetadata $game.FullName
    $gameList += @{
        title = $metadata.Name
        gameURL = $metadata.URL
        genres = $metadata.Genres
        images = @{
            icon = $metadata.IconURL
            poster = $metadata.PosterURL
        }
    }
}
```

#### 2. Data Filtering and Validation
- **Duplicate Removal**: Eliminate duplicate game entries
- **URL Validation**: Ensure all game URLs are accessible
- **Image Verification**: Check that icon and poster URLs are valid
- **Genre Standardization**: Normalize genre names to consistent format

#### 3. Size Optimization
- **Image Compression**: Optimize image URLs for faster loading
- **Metadata Trimming**: Remove unnecessary fields
- **JSON Minification**: Reduce file size while maintaining readability
- **Cache Optimization**: Structure data for efficient caching

#### 4. Final Output Structure
The optimized JSON contains only the essential four fields for each game:

```json
{
    "title": "Fruits into Baskets",
    "gameURL": "https://playgama.com/export/game/fruits-into-baskets?clid=p_db0585d2-2da5-4641-b768-657993e9122d",
    "genres": ["puzzle", "food", "logic", "easy"],
    "images": {
        "icon": "https://playgama.com/cdn-cgi/imagedelivery/LN2S-4p3-GgZvEx3IPaKUA/9071a445-5cd8-4965-3bba-3fb1ff08d900/enlarged",
        "poster": "https://playgama.com/cdn-cgi/imagedelivery/LN2S-4p3-GgZvEx3IPaKUA/537e972d-b7f4-449a-2795-0c63e717a200/enlarged"
    }
}
```

### Script Files Location
- **Main Script**: `PluginStream-Games/UpdateGameList.ps1`
- **Optimizer**: `PluginStream-Games/optimize_games.bat`
- **Raw Data**: `PluginStream-Games/games.json`
- **Optimized Output**: `PluginStream-Games/games_final_lite.json`

### Automation Benefits
- **Reduced File Size**: Optimized JSON loads faster in the app
- **Better Performance**: Fewer fields mean quicker parsing
- **Consistent Format**: Standardized structure across all games
- **Easy Maintenance**: Automated updates reduce manual work

## Important Notes

1. **Cache Duration**: Games are cached until GitHub file changes
2. **Offline Support**: Full offline functionality with cached games
3. **Featured Pattern**: Every 5th game gets featured treatment
4. **Network Optimization**: Smart caching minimizes data usage
5. **Error Recovery**: Graceful fallback to cached data
6. **Performance**: Instant loading with cached data
7. **Security**: All games load in secure WebView sandbox
8. **Automation**: Scripts ensure game list stays current with minimal manual intervention
9. **Optimization**: Lite JSON format reduces app loading time and data usage

---
**Licensed under the MIT License © 2026 Abdul Mueed**
