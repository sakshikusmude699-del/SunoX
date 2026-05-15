package com.soundamplifier.data;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile AudiogramDao _audiogramDao;

  private volatile CustomPresetDao _customPresetDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(5) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `audiogram_profiles` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `accountId` TEXT NOT NULL, `label` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `leftEarThresholds` TEXT NOT NULL, `rightEarThresholds` TEXT NOT NULL, `leftEarGains` TEXT NOT NULL, `rightEarGains` TEXT NOT NULL, `noiseReductionLevel` INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_audiogram_profiles_accountId_createdAt` ON `audiogram_profiles` (`accountId`, `createdAt`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `custom_presets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `accountId` TEXT NOT NULL, `name` TEXT NOT NULL, `boostQuietSounds` REAL NOT NULL, `masterGain` REAL NOT NULL, `lowBoostDb` REAL NOT NULL, `highBoostDb` REAL NOT NULL, `createdAt` INTEGER NOT NULL, `iconKey` TEXT NOT NULL, `builtInPresetId` TEXT)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_custom_presets_accountId_createdAt` ON `custom_presets` (`accountId`, `createdAt`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'b42428f9a0346fa94af10d2cbf1d0005')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `audiogram_profiles`");
        db.execSQL("DROP TABLE IF EXISTS `custom_presets`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsAudiogramProfiles = new HashMap<String, TableInfo.Column>(9);
        _columnsAudiogramProfiles.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAudiogramProfiles.put("accountId", new TableInfo.Column("accountId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAudiogramProfiles.put("label", new TableInfo.Column("label", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAudiogramProfiles.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAudiogramProfiles.put("leftEarThresholds", new TableInfo.Column("leftEarThresholds", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAudiogramProfiles.put("rightEarThresholds", new TableInfo.Column("rightEarThresholds", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAudiogramProfiles.put("leftEarGains", new TableInfo.Column("leftEarGains", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAudiogramProfiles.put("rightEarGains", new TableInfo.Column("rightEarGains", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAudiogramProfiles.put("noiseReductionLevel", new TableInfo.Column("noiseReductionLevel", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysAudiogramProfiles = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesAudiogramProfiles = new HashSet<TableInfo.Index>(1);
        _indicesAudiogramProfiles.add(new TableInfo.Index("index_audiogram_profiles_accountId_createdAt", false, Arrays.asList("accountId", "createdAt"), Arrays.asList("ASC", "ASC")));
        final TableInfo _infoAudiogramProfiles = new TableInfo("audiogram_profiles", _columnsAudiogramProfiles, _foreignKeysAudiogramProfiles, _indicesAudiogramProfiles);
        final TableInfo _existingAudiogramProfiles = TableInfo.read(db, "audiogram_profiles");
        if (!_infoAudiogramProfiles.equals(_existingAudiogramProfiles)) {
          return new RoomOpenHelper.ValidationResult(false, "audiogram_profiles(com.soundamplifier.data.AudiogramProfile).\n"
                  + " Expected:\n" + _infoAudiogramProfiles + "\n"
                  + " Found:\n" + _existingAudiogramProfiles);
        }
        final HashMap<String, TableInfo.Column> _columnsCustomPresets = new HashMap<String, TableInfo.Column>(10);
        _columnsCustomPresets.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomPresets.put("accountId", new TableInfo.Column("accountId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomPresets.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomPresets.put("boostQuietSounds", new TableInfo.Column("boostQuietSounds", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomPresets.put("masterGain", new TableInfo.Column("masterGain", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomPresets.put("lowBoostDb", new TableInfo.Column("lowBoostDb", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomPresets.put("highBoostDb", new TableInfo.Column("highBoostDb", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomPresets.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomPresets.put("iconKey", new TableInfo.Column("iconKey", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomPresets.put("builtInPresetId", new TableInfo.Column("builtInPresetId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCustomPresets = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCustomPresets = new HashSet<TableInfo.Index>(1);
        _indicesCustomPresets.add(new TableInfo.Index("index_custom_presets_accountId_createdAt", false, Arrays.asList("accountId", "createdAt"), Arrays.asList("ASC", "ASC")));
        final TableInfo _infoCustomPresets = new TableInfo("custom_presets", _columnsCustomPresets, _foreignKeysCustomPresets, _indicesCustomPresets);
        final TableInfo _existingCustomPresets = TableInfo.read(db, "custom_presets");
        if (!_infoCustomPresets.equals(_existingCustomPresets)) {
          return new RoomOpenHelper.ValidationResult(false, "custom_presets(com.soundamplifier.data.CustomPreset).\n"
                  + " Expected:\n" + _infoCustomPresets + "\n"
                  + " Found:\n" + _existingCustomPresets);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "b42428f9a0346fa94af10d2cbf1d0005", "ffb89c8372a93ae085598e892b424c7d");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "audiogram_profiles","custom_presets");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `audiogram_profiles`");
      _db.execSQL("DELETE FROM `custom_presets`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(AudiogramDao.class, AudiogramDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(CustomPresetDao.class, CustomPresetDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public AudiogramDao audiogramDao() {
    if (_audiogramDao != null) {
      return _audiogramDao;
    } else {
      synchronized(this) {
        if(_audiogramDao == null) {
          _audiogramDao = new AudiogramDao_Impl(this);
        }
        return _audiogramDao;
      }
    }
  }

  @Override
  public CustomPresetDao customPresetDao() {
    if (_customPresetDao != null) {
      return _customPresetDao;
    } else {
      synchronized(this) {
        if(_customPresetDao == null) {
          _customPresetDao = new CustomPresetDao_Impl(this);
        }
        return _customPresetDao;
      }
    }
  }
}
