package com.example.android.bluetoothlegatt;

import android.database.Cursor;
import androidx.lifecycle.LiveData;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Exception;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@SuppressWarnings({"unchecked", "deprecation"})
public final class WordDao_Impl implements WordDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Word> __insertionAdapterOfWord;

  private final EntityDeletionOrUpdateAdapter<Word> __deletionAdapterOfWord;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public WordDao_Impl(RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfWord = new EntityInsertionAdapter<Word>(__db) {
      @Override
      public String createQuery() {
        return "INSERT OR IGNORE INTO `word_table` (`word`) VALUES (?)";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, Word value) {
        if (value.getWord() == null) {
          stmt.bindNull(1);
        } else {
          stmt.bindString(1, value.getWord());
        }
      }
    };
    this.__deletionAdapterOfWord = new EntityDeletionOrUpdateAdapter<Word>(__db) {
      @Override
      public String createQuery() {
        return "DELETE FROM `word_table` WHERE `word` = ?";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, Word value) {
        if (value.getWord() == null) {
          stmt.bindNull(1);
        } else {
          stmt.bindString(1, value.getWord());
        }
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        final String _query = "DELETE FROM word_table";
        return _query;
      }
    };
  }

  @Override
  public void insert(final Word word) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfWord.insert(word);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void delete(final Word word) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __deletionAdapterOfWord.handle(word);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void deleteAll() {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAll.acquire();
    __db.beginTransaction();
    try {
      _stmt.executeUpdateDelete();
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
      __preparedStmtOfDeleteAll.release(_stmt);
    }
  }

  @Override
  public LiveData<List<Word>> getAlphabetizedWords() {
    final String _sql = "SELECT * from word_table ORDER BY word ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return __db.getInvalidationTracker().createLiveData(new String[]{"word_table"}, false, new Callable<List<Word>>() {
      @Override
      public List<Word> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfMWord = CursorUtil.getColumnIndexOrThrow(_cursor, "word");
          final List<Word> _result = new ArrayList<Word>(_cursor.getCount());
          while(_cursor.moveToNext()) {
            final Word _item;
            final String _tmpMWord;
            _tmpMWord = _cursor.getString(_cursorIndexOfMWord);
            _item = new Word(_tmpMWord);
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
  public List<Word> getPeriodicAllWords() {
    final String _sql = "SELECT * from word_table";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfMWord = CursorUtil.getColumnIndexOrThrow(_cursor, "word");
      final List<Word> _result = new ArrayList<Word>(_cursor.getCount());
      while(_cursor.moveToNext()) {
        final Word _item;
        final String _tmpMWord;
        _tmpMWord = _cursor.getString(_cursorIndexOfMWord);
        _item = new Word(_tmpMWord);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }
}
