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
public final class AudiogramDao_Impl implements AudiogramDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<AudiogramProfile> __insertionAdapterOfAudiogramProfile;

  private final EntityDeletionOrUpdateAdapter<AudiogramProfile> __deletionAdapterOfAudiogramProfile;

  public AudiogramDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfAudiogramProfile = new EntityInsertionAdapter<AudiogramProfile>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `audiogram_profiles` (`id`,`label`,`createdAt`,`leftEarThresholds`,`rightEarThresholds`,`leftEarGains`,`rightEarGains`,`noiseReductionLevel`) VALUES (nullif(?, 0),?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AudiogramProfile entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getLabel());
        statement.bindLong(3, entity.getCreatedAt());
        statement.bindString(4, entity.getLeftEarThresholds());
        statement.bindString(5, entity.getRightEarThresholds());
        statement.bindString(6, entity.getLeftEarGains());
        statement.bindString(7, entity.getRightEarGains());
        statement.bindLong(8, entity.getNoiseReductionLevel());
      }
    };
    this.__deletionAdapterOfAudiogramProfile = new EntityDeletionOrUpdateAdapter<AudiogramProfile>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `audiogram_profiles` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AudiogramProfile entity) {
        statement.bindLong(1, entity.getId());
      }
    };
  }

  @Override
  public Object insertProfile(final AudiogramProfile profile,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfAudiogramProfile.insertAndReturnId(profile);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteProfile(final AudiogramProfile profile,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfAudiogramProfile.handle(profile);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<AudiogramProfile>> getAllProfiles() {
    final String _sql = "SELECT * FROM audiogram_profiles ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"audiogram_profiles"}, new Callable<List<AudiogramProfile>>() {
      @Override
      @NonNull
      public List<AudiogramProfile> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfLabel = CursorUtil.getColumnIndexOrThrow(_cursor, "label");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfLeftEarThresholds = CursorUtil.getColumnIndexOrThrow(_cursor, "leftEarThresholds");
          final int _cursorIndexOfRightEarThresholds = CursorUtil.getColumnIndexOrThrow(_cursor, "rightEarThresholds");
          final int _cursorIndexOfLeftEarGains = CursorUtil.getColumnIndexOrThrow(_cursor, "leftEarGains");
          final int _cursorIndexOfRightEarGains = CursorUtil.getColumnIndexOrThrow(_cursor, "rightEarGains");
          final int _cursorIndexOfNoiseReductionLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "noiseReductionLevel");
          final List<AudiogramProfile> _result = new ArrayList<AudiogramProfile>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AudiogramProfile _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpLabel;
            _tmpLabel = _cursor.getString(_cursorIndexOfLabel);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final String _tmpLeftEarThresholds;
            _tmpLeftEarThresholds = _cursor.getString(_cursorIndexOfLeftEarThresholds);
            final String _tmpRightEarThresholds;
            _tmpRightEarThresholds = _cursor.getString(_cursorIndexOfRightEarThresholds);
            final String _tmpLeftEarGains;
            _tmpLeftEarGains = _cursor.getString(_cursorIndexOfLeftEarGains);
            final String _tmpRightEarGains;
            _tmpRightEarGains = _cursor.getString(_cursorIndexOfRightEarGains);
            final int _tmpNoiseReductionLevel;
            _tmpNoiseReductionLevel = _cursor.getInt(_cursorIndexOfNoiseReductionLevel);
            _item = new AudiogramProfile(_tmpId,_tmpLabel,_tmpCreatedAt,_tmpLeftEarThresholds,_tmpRightEarThresholds,_tmpLeftEarGains,_tmpRightEarGains,_tmpNoiseReductionLevel);
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
  public Object getLatestProfile(final Continuation<? super AudiogramProfile> $completion) {
    final String _sql = "SELECT * FROM audiogram_profiles ORDER BY createdAt DESC LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<AudiogramProfile>() {
      @Override
      @Nullable
      public AudiogramProfile call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfLabel = CursorUtil.getColumnIndexOrThrow(_cursor, "label");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfLeftEarThresholds = CursorUtil.getColumnIndexOrThrow(_cursor, "leftEarThresholds");
          final int _cursorIndexOfRightEarThresholds = CursorUtil.getColumnIndexOrThrow(_cursor, "rightEarThresholds");
          final int _cursorIndexOfLeftEarGains = CursorUtil.getColumnIndexOrThrow(_cursor, "leftEarGains");
          final int _cursorIndexOfRightEarGains = CursorUtil.getColumnIndexOrThrow(_cursor, "rightEarGains");
          final int _cursorIndexOfNoiseReductionLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "noiseReductionLevel");
          final AudiogramProfile _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpLabel;
            _tmpLabel = _cursor.getString(_cursorIndexOfLabel);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final String _tmpLeftEarThresholds;
            _tmpLeftEarThresholds = _cursor.getString(_cursorIndexOfLeftEarThresholds);
            final String _tmpRightEarThresholds;
            _tmpRightEarThresholds = _cursor.getString(_cursorIndexOfRightEarThresholds);
            final String _tmpLeftEarGains;
            _tmpLeftEarGains = _cursor.getString(_cursorIndexOfLeftEarGains);
            final String _tmpRightEarGains;
            _tmpRightEarGains = _cursor.getString(_cursorIndexOfRightEarGains);
            final int _tmpNoiseReductionLevel;
            _tmpNoiseReductionLevel = _cursor.getInt(_cursorIndexOfNoiseReductionLevel);
            _result = new AudiogramProfile(_tmpId,_tmpLabel,_tmpCreatedAt,_tmpLeftEarThresholds,_tmpRightEarThresholds,_tmpLeftEarGains,_tmpRightEarGains,_tmpNoiseReductionLevel);
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
