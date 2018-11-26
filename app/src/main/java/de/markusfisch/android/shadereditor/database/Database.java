package de.markusfisch.android.shadereditor.database;

import de.markusfisch.android.shadereditor.R;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Locale;
import java.util.Formatter;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION;

public class Database {
	public static final String SHADERS = "shaders";
	public static final String SHADERS_ID = "_id";
	public static final String SHADERS_FRAGMENT_SHADER = "shader";
	public static final String SHADERS_THUMB = "thumb";
	public static final String SHADERS_NAME = "name";
	public static final String SHADERS_CREATED = "created";
	public static final String SHADERS_MODIFIED = "modified";
	public static final String SHADERS_QUALITY = "quality";

	public static final String TEXTURES = "textures";
	public static final String TEXTURES_ID = "_id";
	public static final String TEXTURES_NAME = "name";
	public static final String TEXTURES_WIDTH = "width";
	public static final String TEXTURES_HEIGHT = "height";
	public static final String TEXTURES_RATIO = "ratio";
	public static final String TEXTURES_THUMB = "thumb";
	// public static final String TEXTURES_MATRIX = "matrix";
	public static final String TEXTURES_HASH = "hash";

	public static final String TEXTURES_DIRECTORY = "textures";

	private SQLiteDatabase db;
	private Context dbContext;
	private int textureThumbnailSize;

	// the parent instance of this AsyncTask will never be
	// garbage collected anyway
	@SuppressLint("StaticFieldLeak")
	public void openAsync(final Context context) {
		textureThumbnailSize = Math.round(
				context.getResources().getDisplayMetrics().density * 48f);

		final OpenHelper helper = new OpenHelper(context);
		dbContext = context;

		new AsyncTask<Void, Void, Boolean>() {
			@Override
			protected Boolean doInBackground(Void... nothings) {
				try {
					return (db = helper.getWritableDatabase()) != null;
				} catch (SQLException e) {
					return false;
				}
			}

			@Override
			protected void onPostExecute(Boolean success) {
				if (success) {
					return;
				}

				Toast.makeText(
						context,
						R.string.cannot_open_database,
						Toast.LENGTH_LONG).show();
			}
		}.execute();
	}

	public boolean isOpen() {
		return db != null;
	}

	public static boolean closeIfEmpty(Cursor cursor) {
		if (cursor != null && cursor.moveToFirst()) {
			return false;
		}

		if (cursor != null) {
			cursor.close();
		}

		return true;
	}

	public Cursor getShaders() {
		return db.rawQuery(
				"SELECT " +
						SHADERS_ID + "," +
						SHADERS_THUMB + "," +
						SHADERS_NAME + "," +
						SHADERS_MODIFIED +
						" FROM " + SHADERS +
						" ORDER BY " + SHADERS_ID,
				null);
	}

	public Cursor getTextures() {
		return db.rawQuery(
				"SELECT " +
						TEXTURES_ID + "," +
						TEXTURES_NAME + "," +
						TEXTURES_WIDTH + "," +
						TEXTURES_HEIGHT + "," +
						TEXTURES_THUMB +
						" FROM " + TEXTURES +
						" WHERE " + TEXTURES_RATIO + " = 1" +
						" ORDER BY " + TEXTURES_ID,
				null);
	}

	public Cursor getSamplerCubeTextures() {
		return db.rawQuery(
				"SELECT " +
						TEXTURES_ID + "," +
						TEXTURES_NAME + "," +
						TEXTURES_WIDTH + "," +
						TEXTURES_HEIGHT + "," +
						TEXTURES_THUMB +
						" FROM " + TEXTURES +
						" WHERE " + TEXTURES_RATIO + " = 1.5" +
						" ORDER BY " + TEXTURES_ID,
				null);
	}

	public boolean isShaderAvailable(long id) {
		Cursor cursor = db.rawQuery(
				"SELECT " +
						SHADERS_ID +
						" FROM " + SHADERS +
						" WHERE " + SHADERS_ID + " = ?",
				new String[]{String.valueOf(id)});
		if (cursor == null) {
			return false;
		}
		boolean exists = cursor.moveToFirst();
		cursor.close();
		return exists;
	}

	public Cursor getShader(long id) {
		return db.rawQuery(
				"SELECT " +
						SHADERS_ID + "," +
						SHADERS_FRAGMENT_SHADER + "," +
						SHADERS_NAME + "," +
						SHADERS_MODIFIED + "," +
						SHADERS_QUALITY +
						" FROM " + SHADERS +
						" WHERE " + SHADERS_ID + " = ?",
				new String[]{String.valueOf(id)});
	}

	public Cursor getRandomShader() {
		return db.rawQuery(
				"SELECT " +
						SHADERS_ID + "," +
						SHADERS_FRAGMENT_SHADER + "," +
						SHADERS_MODIFIED + "," +
						SHADERS_QUALITY +
						" FROM " + SHADERS +
						" ORDER BY RANDOM() LIMIT 1",
				null);
	}

	public byte[] getThumbnail(long id) {
		Cursor cursor = db.rawQuery(
				"SELECT " +
						SHADERS_THUMB +
						" FROM " + SHADERS +
						" WHERE " + SHADERS_ID + " = ?",
				new String[]{String.valueOf(id)});

		if (closeIfEmpty(cursor)) {
			return null;
		}

		byte thumbnail[] = cursor.getBlob(
				cursor.getColumnIndex(SHADERS_THUMB));

		cursor.close();

		return thumbnail;
	}

	public long getFirstShaderId() {
		Cursor cursor = db.rawQuery(
				"SELECT " +
						SHADERS_ID +
						" FROM " + SHADERS +
						" ORDER BY " + SHADERS_ID +
						" LIMIT 1",
				null);

		if (closeIfEmpty(cursor)) {
			return 0;
		}

		long id = cursor.getLong(cursor.getColumnIndex(SHADERS_ID));
		cursor.close();

		return id;
	}

	public Cursor getTexture(long id) {
		return db.rawQuery(
				"SELECT " +
						TEXTURES_NAME + "," +
						TEXTURES_WIDTH + "," +
						TEXTURES_HEIGHT + "," +
						TEXTURES_HASH +
						" FROM " + TEXTURES +
						" WHERE " + TEXTURES_ID + " = ?",
				new String[]{String.valueOf(id)});
	}

	public Bitmap getTextureBitmap(String name) {
		Cursor cursor = db.rawQuery(
				"SELECT " +
						TEXTURES_HASH +
						" FROM " + TEXTURES +
						" WHERE " + TEXTURES_NAME + " = ?",
				new String[]{name});
		Bitmap bm = getTextureBitmap(cursor);
		cursor.close();
		return bm;
	}

	public Bitmap getTextureBitmap(Cursor cursor) {
		if (closeIfEmpty(cursor)) {
			return null;
		}

		try {
			return textureFromCursor(cursor, dbContext);
		} catch(IOException e) {
			Toast.makeText(
					dbContext,
					R.string.cannot_load_texture,
					Toast.LENGTH_LONG).show();
			return null;
		}
	}

	public static long insertShader(
			SQLiteDatabase db,
			String shader,
			String name,
			byte thumbnail[],
			float quality) {
		String now = currentTime();

		ContentValues cv = new ContentValues();
		cv.put(SHADERS_FRAGMENT_SHADER, shader);
		cv.put(SHADERS_THUMB, thumbnail);
		cv.put(SHADERS_NAME, name);
		cv.put(SHADERS_CREATED, now);
		cv.put(SHADERS_MODIFIED, now);
		cv.put(SHADERS_QUALITY, quality);

		return db.insert(SHADERS, null, cv);
	}

	public long insertShader(
			String shader,
			byte[] thumbnail,
			float quality) {
		return insertShader(db, shader, null, thumbnail, quality);
	}

	public long insertShader(
			Context context,
			String shader,
			String name) {
		return insertShader(db, shader, name,
				loadBitmapResource(context, R.drawable.thumbnail_new_shader), 1f);
	}

	public long insertNewShader(Context context) {
		return insertShaderFromResource(
				context,
				null,
				R.raw.new_shader,
				R.drawable.thumbnail_new_shader,
				1f);
	}

	public long insertShaderFromResource(
			Context context,
			String name,
			int sourceId,
			int thumbId,
			float quality) {
		try {
			return insertShader(
					db,
					loadRawResource(context, sourceId),
					name,
					loadBitmapResource(context, thumbId),
					quality);
		} catch (IOException e) {
			return 0;
		}
	}

	private static long insertTexture(
			SQLiteDatabase db,
			String name,
			Bitmap bitmap,
			int thumbnailSize,
			Context context) {
		Bitmap thumbnail;

		try {
			thumbnail = Bitmap.createScaledBitmap(
					bitmap,
					thumbnailSize,
					thumbnailSize,
					true);
		} catch (IllegalArgumentException e) {
			return 0;
		}

		int w = bitmap.getWidth();
		int h = bitmap.getHeight();

		ContentValues cv = new ContentValues();
		cv.put(TEXTURES_NAME, name);
		cv.put(TEXTURES_WIDTH, w);
		cv.put(TEXTURES_HEIGHT, h);
		cv.put(TEXTURES_RATIO, calculateRatio(w, h));
		cv.put(TEXTURES_THUMB, bitmapToPng(thumbnail));

		String hash;

		try {
			byte[] data = bitmapToPng(bitmap);
			hash = hashBlob(data);
			File file = getTextureFile(hash, context);
			BufferedOutputStream buf = new BufferedOutputStream(new FileOutputStream(file));
			buf.write(data);
			buf.close();
		} catch(IOException e) {
			e.printStackTrace();
			Toast.makeText(
					context,
					R.string.cannot_store_texture,
					Toast.LENGTH_LONG).show();

			return 0;
		}

		cv.put(TEXTURES_HASH, hash);

		return db.insert(TEXTURES, null, cv);
	}

	public long insertTexture(String name, Bitmap bitmap) {
		return insertTexture(
				db,
				name,
				bitmap,
				textureThumbnailSize,
				dbContext);
	}

	public void updateShader(
			long id,
			String shader,
			byte thumbnail[],
			float quality) {
		ContentValues cv = new ContentValues();
		cv.put(SHADERS_FRAGMENT_SHADER, shader);
		cv.put(SHADERS_MODIFIED, currentTime());
		cv.put(SHADERS_QUALITY, quality);

		if (thumbnail != null) {
			cv.put(SHADERS_THUMB, thumbnail);
		}

		db.update(SHADERS, cv, SHADERS_ID + " = ?",
				new String[]{String.valueOf(id)});
	}

	public void updateShaderQuality(
			long id,
			float quality) {
		ContentValues cv = new ContentValues();
		cv.put(SHADERS_QUALITY, quality);

		db.update(SHADERS, cv, SHADERS_ID + " = ?",
				new String[]{String.valueOf(id)});
	}

	public void updateShaderName(long id, String name) {
		ContentValues cv = new ContentValues();
		cv.put(SHADERS_NAME, name);
		db.update(SHADERS, cv, SHADERS_ID + " = ?",
				new String[]{String.valueOf(id)});
	}

	public void removeShader(long id) {
		db.delete(SHADERS, SHADERS_ID + " = ?",
				new String[]{String.valueOf(id)});
	}

	public void removeTexture(long id) {
		db.delete(TEXTURES, TEXTURES_ID + " = ?",
				new String[]{String.valueOf(id)});
	}

	private static String currentTime() {
		return new SimpleDateFormat(
				"yyyy-MM-dd HH:mm:ss",
				Locale.US).format(new Date());
	}

	private static Bitmap textureFromCursor(Cursor cursor, Context context) throws IOException {
		String hash = cursor.getString(cursor.getColumnIndex(TEXTURES_HASH));
		File file = getTextureFile(hash, context);
		byte[] data = new byte[(int)file.length()];

		BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
		buf.read(data, 0, data.length);
		buf.close();

		return BitmapFactory.decodeByteArray(data, 0, data.length);
	}

	private static String loadRawResource(
			Context context,
			int id) throws IOException {
		InputStream in = null;
		try {
			in = context.getResources().openRawResource(id);
			int l = in.available();
			byte b[] = new byte[l];
			return in.read(b) == l ? new String(b, "UTF-8") : null;
		} finally {
			if (in != null) {
				in.close();
			}
		}
	}

	private static byte[] loadBitmapResource(Context context, int id) {
		return bitmapToPng(BitmapFactory.decodeResource(
				context.getResources(),
				id));
	}

	private static byte[] bitmapToPng(Bitmap bitmap) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
		return out.toByteArray();
	}

	private static float calculateRatio(int width, int height) {
		// round to two decimal places to avoid problems with
		// rounding errors; the query will filter precisely 1 or 1.5
		return Math.round(((float) height / width) * 100f) / 100f;
	}

	private static String hashBlob(final byte[] blob) {
		byte[] hash;

		try {
			hash = MessageDigest.getInstance("SHA-1").digest(blob);
		} catch(NoSuchAlgorithmException e) {
			// no idea how to deal with this; not happening anyway.
			e.printStackTrace();
			return null;
		}

		Formatter formatter = new Formatter();

		for (byte b : hash) {
			formatter.format("%02x", b);
		}

		return formatter.toString();
	}

	private static void requestReadPermission(Context context) {
		String permission = Manifest.permission.READ_EXTERNAL_STORAGE;
		if (ContextCompat.checkSelfPermission(context, permission) !=
				PackageManager.PERMISSION_GRANTED) {
			Activity activity;
			try {
				activity = (Activity) context;
			} catch (ClassCastException e) {
				return;
			}
			ActivityCompat.requestPermissions(
					activity,
					new String[]{permission},
					1);
		}
	}

	private static File getTexturesDirectory(Context context) throws IOException {
		requestReadPermission(context);

		File dir = context.getExternalFilesDir(null);
		dir = new File(dir, TEXTURES_DIRECTORY);
		dir.mkdirs();

		if (!dir.isDirectory()) {
			throw new IOException(context.getString(
					R.string.path_is_no_directory, dir));
		}

		return dir;
	}

	private static File getTextureFile(final String hash, Context context) throws IOException {
		return new File(getTexturesDirectory(context), String.format("%s.png", hash));
	}

	private void createShadersTable(SQLiteDatabase db, Context context) {
		db.execSQL("DROP TABLE IF EXISTS " + SHADERS);
		db.execSQL("CREATE TABLE " + SHADERS + " (" +
				SHADERS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
				SHADERS_FRAGMENT_SHADER + " TEXT NOT NULL," +
				SHADERS_THUMB + " BLOB," +
				SHADERS_NAME + " TEXT," +
				SHADERS_CREATED + " DATETIME," +
				SHADERS_MODIFIED + " DATETIME," +
				SHADERS_QUALITY + " REAL );");

		insertInitalShaders(db, context);
	}

	private static void addShadersQuality(SQLiteDatabase db) {
		db.execSQL("ALTER TABLE " + SHADERS +
				" ADD COLUMN " + SHADERS_QUALITY + " REAL;");
		db.execSQL("UPDATE " + SHADERS +
				" SET " + SHADERS_QUALITY + " = 1;");
	}

	private void insertInitalShaders(SQLiteDatabase db, Context context) {
		try {
			insertShader(
					db,
					loadRawResource(
							context,
							R.raw.default_shader),
					context.getString(R.string.default_shader),
					loadBitmapResource(
							context,
							R.drawable.thumbnail_default),
					1f);
		} catch (IOException e) {
			// shouldn't ever happen in production
			// and nothing can be done if it does
		}
	}

	private void createTexturesTable(SQLiteDatabase db, Context context) {
		db.execSQL("DROP TABLE IF EXISTS " + TEXTURES);
		db.execSQL("CREATE TABLE " + TEXTURES + " (" +
				TEXTURES_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
				TEXTURES_NAME + " TEXT NOT NULL UNIQUE," +
				TEXTURES_WIDTH + " INTEGER," +
				TEXTURES_HEIGHT + " INTEGER," +
				TEXTURES_RATIO + " REAL," +
				TEXTURES_THUMB + " BLOB," +
				TEXTURES_HASH + " TEXT);");

		insertInitalTextures(db, context);
	}

	private static void addTexturesWidthHeightRatio(SQLiteDatabase db, Context context) {
		db.execSQL("ALTER TABLE " + TEXTURES +
				" ADD COLUMN " + TEXTURES_WIDTH + " INTEGER;");
		db.execSQL("ALTER TABLE " + TEXTURES +
				" ADD COLUMN " + TEXTURES_HEIGHT + " INTEGER;");
		db.execSQL("ALTER TABLE " + TEXTURES +
				" ADD COLUMN " + TEXTURES_RATIO + " REAL;");

		Cursor cursor = db.rawQuery(
				"SELECT " +
						TEXTURES_ID + "," +
						TEXTURES_HASH +
						" FROM " + TEXTURES,
				null);

		if (closeIfEmpty(cursor)) {
			return;
		}

		do {
			Bitmap bm;

			try {
				bm = textureFromCursor(cursor, context);
			} catch(IOException e) {
				continue;
			}

			if (bm == null) {
				continue;
			}

			int width = bm.getWidth();
			int height = bm.getHeight();
			float ratio = calculateRatio(width, height);
			bm.recycle();

			db.execSQL("UPDATE " + TEXTURES +
					" SET " +
					TEXTURES_WIDTH + " = " + width + ", " +
					TEXTURES_HEIGHT + " = " + height + ", " +
					TEXTURES_RATIO + " = " + ratio +
					" WHERE " + TEXTURES_ID + " = " + cursor.getLong(
					cursor.getColumnIndex(TEXTURES_ID)) + ";");

		} while (cursor.moveToNext());

		cursor.close();
	}

	private static void addShaderNames(SQLiteDatabase db) {
		db.execSQL("ALTER TABLE " + SHADERS +
				" ADD COLUMN " + SHADERS_NAME + " TEXT;");
	}

	private void insertInitalTextures(SQLiteDatabase db, Context context) {
		Database.insertTexture(
				db,
				context.getString(R.string.texture_name_noise),
				BitmapFactory.decodeResource(
						context.getResources(),
						R.drawable.texture_noise),
				textureThumbnailSize,
				dbContext);
	}

	private class OpenHelper extends SQLiteOpenHelper {
		private final Context context;

		@Override
		public void onCreate(SQLiteDatabase db) {
			createShadersTable(db, context);
			createTexturesTable(db, context);
		}

		@Override
		public void onDowngrade(
				SQLiteDatabase db,
				int oldVersion,
				int newVersion) {
			// without onDowngrade(), a downgrade will throw
			// an exception; can never happen in production
		}

		@Override
		public void onUpgrade(
				SQLiteDatabase db,
				int oldVersion,
				int newVersion) {
			if (oldVersion < 2) {
				createTexturesTable(db, context);
				insertInitalShaders(db, context);
			}

			if (oldVersion < 3) {
				addShadersQuality(db);
			}

			if (oldVersion < 4) {
				addTexturesWidthHeightRatio(db, context);
			}

			if (oldVersion < 5) {
				addShaderNames(db);
			}
		}

		private OpenHelper(Context context) {
			super(context, "shaders.db", null, 5);
			this.context = context;
		}
	}
}
