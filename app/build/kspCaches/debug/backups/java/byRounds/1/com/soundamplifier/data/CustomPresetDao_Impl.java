package com.soundamplifier.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class CustomPresetDao_Impl implements CustomPresetDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<CustomPreset> __insertionAdapterOfCustomPreset;

  private final EntityInsertionAdapter<CustomPreset> __insertionAdapterOfCustomPreset_1;

  private final EntityDeletionOrUpdateAdapter<CustomPreset> __deletionAdapterOfCustomPreset;

  private final EntityDeletionOrUpdateAdapter<CustomPreset> __updateAdapterOfCustomPreset;

  private final SharedSQLiteStatement __preparedStmtOfDeleteByNameForAccount;

  private final SharedSQLiteStatement __preparedStmtOfDeleteBuiltInOverride;

  public CustomPresetDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfCustomPreset = new EntityInsertionAdapter<CustomPreset>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `custom_presets` (`id`,`accountId`,`name`,`boostQuietSounds`,`masterGain`,`lowBoostDb`,`highBoostDb`,`createdAt`,`iconKey`,`builtInPresetId`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CustomPreset entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getAccountId());
        statement.bindString(3, entity.getName());
        statement.bindDouble(4, entity.getBoostQuietSounds());
        statement.bindDouble(5, entity.getMasterGain());
        statement.bindDouble(6, entity.getLowBoostDb());
        statement.bindDouble(7, entity.getHighBoostDb());
        statement.bindLong(8, entity.getCreatedAt());
        statement.bindString(9, entity.getIconKey());
        if (entity.getBuiltInPresetId() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getBuiltInPresetId());
        }
      }
    };
    this.__insertionAdapterOfCustomPreset_1 = new EntityInsertionAdapter<CustomPreset>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `custom_presets` (`id`,`accountId`,`name`,`boostQuietSounds`,`masterGain`,`lowBoostDb`,`highBoostDb`,`createdAt`,`iconKey`,`builtInPresetId`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CustomPreset entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getAccountId());
        statement.bindString(3, entity.getName());
        statement.bindDouble(4, entity.getBoostQuietSounds());
        statement.bindDouble(5, entity.getMasterGain());
        statement.bindDouble(6, entity.getLowBoostDb());
        statement.bindDouble(7, entity.getHighBoostDb());
        statement.bindLong(8, entity.getCreatedAt());
        statement.bindString(9, entity.getIconKey());
        if (entity.getBuiltInPresetId() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getBuiltInPresetId());
        }
      }
    };
    this.__deletionAdapterOfCustomPreset = new EntityDeletionOrUpdateAdapter<CustomPreset>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `custom_presets` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CustomPreset entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfCustomPreset = new EntityDeletionOrUpdateAdapter<CustomPreset>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `custom_presets` SET `id` = ?,`accountId` = ?,`name` = ?,`boostQuietSounds` = ?,`masterGain` = ?,`lowBoostDb` = ?,`highBoostDb` = ?,`createdAt` = ?,`iconKey` = ?,`builtInPresetId` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CustomPreset entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getAccountId());
        statement.bindString(3, entity.getName());
        statement.bindDouble(4, entity.getBoostQuietSounds());
        statement.bindDouble(5, entity.getMasterGain());
        statement.bindDouble(6, entity.getLowBoostDb());
        statement.bindDouble(7, entity.getHighBoostDb());
        statement.bindLong(8, entity.getCreatedAt());
        statement.bindString(9, entity.getIconKey());
        if (entity.getBuiltInPresetId() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getBuiltInPresetId());
        }
        statement.bindLong(11, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteByNameForAccount = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM custom_presets WHERE accountId = ? AND name = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteBuiltInOverride = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM custom_presets WHERE accountId = ? AND builtInPresetId = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final CustomPreset preset, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfCustomPreset.insertAndReturnId(preset);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertOrReplace(final CustomPreset preset,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfCustomPreset_1.insertAndReturnId(preset);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final CustomPreset preset, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfCustomPreset.handle(preset);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final CustomPreset preset, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfCustomPreset.handle(preset);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteByNameForAccount(final String accountId, final String name,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteByNameForAccount.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, accountId);
        _argIndex = 2;
        _stmt.bindString(_argIndex, name);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteByNameForAccount.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteBuiltInOverride(final String accountId, final String builtInId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteBuiltInOverride.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, accountId);
        _argIndex = 2;
        _stmt.bindString(_argIndex, builtInId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteBuiltInOverride.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<CustomPreset>> getAllPresetsFlowForAccount(final String accountId) {
    final String _sql = "SELECT * FROM custom_presets WHERE accountId = ? ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, accountId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"custom_presets"}, new Callable<List<CustomPreset>>() {
      @Override
      @NonNull
      public List<CustomPreset> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfBoostQuietSounds = CursorUtil.getColumnIndexOrThrow(_cursor, "boostQuietSounds");
          final int _cursorIndexOfMasterGain = CursorUtil.getColumnIndexOrThrow(_cursor, "masterGain");
          final int _cursorIndexOfLowBoostDb = CursorUtil.getColumnIndexOrThrow(_cursor, "lowBoostDb");
          final int _cursorIndexOfHighBoostDb = CursorUtil.getColumnIndexOrThrow(_cursor, "highBoostDb");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfIconKey = CursorUtil.getColumnIndexOrThrow(_cursor, "iconKey");
          final int _cursorIndexOfBuiltInPresetId = CursorUtil.getColumnIndexOrThrow(_cursor, "builtInPresetId");
          final List<CustomPreset> _result = new ArrayList<CustomPreset>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CustomPreset _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpAccountId;
            _tmpAccountId = _cursor.getString(_cursorIndexOfAccountId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final float _tmpBoostQuietSounds;
            _tmpBoostQuietSounds = _cursor.getFloat(_cursorIndexOfBoostQuietSounds);
            final float _tmpMasterGain;
            _tmpMasterGain = _cursor.getFloat(_cursorIndexOfMasterGain);
            final float _tmpLowBoostDb;
            _tmpLowBoostDb = _cursor.getFloat(_cursorIndexOfLowBoostDb);
            final float _tmpHighBoostDb;
            _tmpHighBoostDb = _cursor.getFloat(_cursorIndexOfHighBoostDb);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final String _tmpIconKey;
            _tmpIconKey = _cursor.getString(_cursorIndexOfIconKey);
            final String _tmpBuiltInPresetId;
            if (_cursor.isNull(_cursorIndexOfBuiltInPresetId)) {
              _tmpBuiltInPresetId = null;
            } else {
              _tmpBuiltInPresetId = _cursor.getString(_cursorIndexOfBuiltInPresetId);
            }
            _item = new CustomPreset(_tmpId,_tmpAccountId,_tmpName,_tmpBoostQuietSounds,_tmpMasterGain,_tmpLowBoostDb,_tmpHighBoostDb,_tmpCreatedAt,_tmpIconKey,_tmpBuiltInPresetId);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getAllPresetsForAccount(final String accountId,
      final Continuation<? super List<CustomPreset>> $completion) {
    final String _sql = "SELECT * FROM custom_presets WHERE accountId = ? ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, accountId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<CustomPreset>>() {
      @Override
      @NonNull
      public List<CustomPreset> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfBoostQuietSounds = CursorUtil.getColumnIndexOrThrow(_cursor, "boostQuietSounds");
          final int _cursorIndexOfMasterGain = CursorUtil.getColumnIndexOrThrow(_cursor, "masterGain");
          final int _cursorIndexOfLowBoostDb = CursorUtil.getColumnIndexOrThrow(_cursor, "lowBoostDb");
          final int _cursorIndexOfHighBoostDb = CursorUtil.getColumnIndexOrThrow(_cursor, "highBoostDb");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfIconKey = CursorUtil.getColumnIndexOrThrow(_cursor, "iconKey");
          final int _cursorIndexOfBuiltInPresetId = CursorUtil.getColumnIndexOrThrow(_cursor, "builtInPresetId");
          final List<CustomPreset> _result = new ArrayList<CustomPreset>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CustomPreset _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpAccountId;
            _tmpAccountId = _cursor.getString(_cursorIndexOfAccountId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final float _tmpBoostQuietSounds;
            _tmpBoostQuietSounds = _cursor.getFloat(_cursorIndexOfBoostQuietSounds);
            final float _tmpMasterGain;
            _tmpMasterGain = _cursor.getFloat(_cursorIndexOfMasterGain);
            final float _tmpLowBoostDb;
            _tmpLowBoostDb = _cursor.getFloat(_cursorIndexOfLowBoostDb);
            final float _tmpHighBoostDb;
            _tmpHighBoostDb = _cursor.getFloat(_cursorIndexOfHighBoostDb);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final String _tmpIconKey;
            _tmpIconKey = _cursor.getString(_cursorIndexOfIconKey);
            final String _tmpBuiltInPresetId;
            if (_cursor.isNull(_cursorIndexOfBuiltInPresetId)) {
              _tmpBuiltInPresetId = null;
            } else {
              _tmpBuiltInPresetId = _cursor.getString(_cursorIndexOfBuiltInPresetId);
            }
            _item = new CustomPreset(_tmpId,_tmpAccountId,_tmpName,_tmpBoostQuietSounds,_tmpMasterGain,_tmpLowBoostDb,_tmpHighBoostDb,_tmpCreatedAt,_tmpIconKey,_tmpBuiltInPresetId);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getBuiltInOverride(final String accountId, final String builtInId,
      final Continuation<? super CustomPreset> $completion) {
    final String _sql = "SELECT * FROM custom_presets WHERE accountId = ? AND builtInPresetId = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, accountId);
    _argIndex = 2;
    _statement.bindString(_argIndex, builtInId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<CustomPreset>() {
      @Override
      @Nullable
      public CustomPreset call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfBoostQuietSounds = CursorUtil.getColumnIndexOrThrow(_cursor, "boostQuietSounds");
          final int _cursorIndexOfMasterGain = CursorUtil.getColumnIndexOrThrow(_cursor, "masterGain");
          final int _cursorIndexOfLowBoostDb = CursorUtil.getColumnIndexOrThrow(_cursor, "lowBoostDb");
          final int _cursorIndexOfHighBoostDb = CursorUtil.getColumnIndexOrThrow(_cursor, "highBoostDb");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfIconKey = CursorUtil.getColumnIndexOrThrow(_cursor, "iconKey");
          final int _cursorIndexOfBuiltInPresetId = CursorUtil.getColumnIndexOrThrow(_cursor, "builtInPresetId");
          final CustomPreset _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpAccountId;
            _tmpAccountId = _cursor.getString(_cursorIndexOfAccountId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final float _tmpBoostQuietSounds;
            _tmpBoostQuietSounds = _cursor.getFloat(_cursorIndexOfBoostQuietSounds);
            final float _tmpMasterGain;
            _tmpMasterGain = _cursor.getFloat(_cursorIndexOfMasterGain);
            final float _tmpLowBoostDb;
            _tmpLowBoostDb = _cursor.getFloat(_cursorIndexOfLowBoostDb);
            final float _tmpHighBoostDb;
            _tmpHighBoostDb = _cursor.getFloat(_cursorIndexOfHighBoostDb);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final String _tmpIconKey;
            _tmpIconKey = _cursor.getString(_cursorIndexOfIconKey);
            final String _tmpBuiltInPresetId;
            if (_cursor.isNull(_cursorIndexOfBuiltInPresetId)) {
              _tmpBuiltInPresetId = null;
            } else {
              _tmpBuiltInPresetId = _cursor.getString(_cursorIndexOfBuiltInPresetId);
            }
            _result = new CustomPreset(_tmpId,_tmpAccountId,_tmpName,_tmpBoostQuietSounds,_tmpMasterGain,_tmpLowBoostDb,_tmpHighBoostDb,_tmpCreatedAt,_tmpIconKey,_tmpBuiltInPresetId);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
