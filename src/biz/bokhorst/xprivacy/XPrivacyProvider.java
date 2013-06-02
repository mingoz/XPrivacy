package biz.bokhorst.xprivacy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Binder;
import android.text.TextUtils;

public class XPrivacyProvider extends ContentProvider {

	public static final String AUTHORITY = "biz.bokhorst.xprivacy.provider";
	public static final String PATH_RESTRICTIONS = "restrictions";
	public static final String PATH_LASTUSED = "lastuse";
	public static final Uri URI_RESTRICTIONS = Uri.parse("content://" + AUTHORITY + "/" + PATH_RESTRICTIONS);
	public static final Uri URI_LASTUSED = Uri.parse("content://" + AUTHORITY + "/" + PATH_LASTUSED);

	public static final String COL_NAME = "Name";
	public static final String COL_RESTRICTED = "Restricted";
	public static final String COL_UID = "Uid";
	public static final String COL_LASTUSED = "LastUsed";

	private static final UriMatcher sUriMatcher;
	private static final int TYPE_RESTRICTIONS = 1;
	private static final int TYPE_LASTUSED = 2;

	static {
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		sUriMatcher.addURI(AUTHORITY, PATH_RESTRICTIONS, TYPE_RESTRICTIONS);
		sUriMatcher.addURI(AUTHORITY, PATH_LASTUSED, TYPE_LASTUSED);
	}

	@Override
	public boolean onCreate() {
		return true;
	}

	@Override
	public String getType(Uri uri) {
		if (sUriMatcher.match(uri) == TYPE_RESTRICTIONS)
			return String.format("vnd.android.cursor.dir/%s.%s", AUTHORITY, PATH_RESTRICTIONS);
		else if (sUriMatcher.match(uri) == TYPE_LASTUSED)
			return String.format("vnd.android.cursor.dir/%s.%s", AUTHORITY, PATH_LASTUSED);
		throw new IllegalArgumentException();
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String restrictionName, String[] selectionArgs, String sortOrder) {
		if (selectionArgs != null) {
			// Get arguments
			SharedPreferences prefs = getContext().getSharedPreferences(AUTHORITY, Context.MODE_PRIVATE);

			if (sUriMatcher.match(uri) == TYPE_RESTRICTIONS && selectionArgs.length == 2) {
				// Get arguments
				int uid = Integer.parseInt(selectionArgs[0]);
				boolean usage = Boolean.parseBoolean(selectionArgs[1]);

				// Update usage count
				if (usage) {
					SharedPreferences.Editor editor = prefs.edit();
					editor.putLong(getUsagePref(uid, restrictionName), new Date().getTime());
					editor.commit();
				}

				// Get restrictions
				String restrictions = prefs.getString(getRestrictionPref(restrictionName), "*");

				// Decode restrictions
				List<String> listRestriction = new ArrayList<String>(Arrays.asList(restrictions.split(",")));
				boolean defaultAllowed = listRestriction.get(0).equals("*");

				// Check if allowed
				boolean allowed = !listRestriction.contains(Integer.toString(uid));
				if (!defaultAllowed)
					allowed = !allowed;

				// Return restriction
				MatrixCursor cursor = new MatrixCursor(new String[] { COL_NAME, COL_RESTRICTED });
				cursor.addRow(new Object[] { restrictionName, Boolean.toString(!allowed) });
				return cursor;
			} else if (sUriMatcher.match(uri) == TYPE_LASTUSED && selectionArgs.length == 1) {
				// Return usage
				int uid = Integer.parseInt(selectionArgs[0]);
				MatrixCursor cursor = new MatrixCursor(new String[] { COL_UID, COL_NAME, COL_LASTUSED });
				cursor.addRow(new Object[] { uid, restrictionName, prefs.getLong(getUsagePref(uid, restrictionName), 0) });
				return cursor;
			}
		}
		throw new IllegalArgumentException();
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		throw new IllegalArgumentException();
	}

	@Override
	public int update(Uri uri, ContentValues values, String restrictionName, String[] selectionArgs) {
		// TODO: register update time?
		if (sUriMatcher.match(uri) == TYPE_RESTRICTIONS) {
			// Check update restriction
			int cuid = Binder.getCallingUid();
			String[] packages = getContext().getPackageManager().getPackagesForUid(cuid);
			List<String> listPackage = new ArrayList<String>(Arrays.asList(packages));
			String packageName = this.getClass().getPackage().getName();
			if (listPackage.contains(packageName)) {
				// Get argument
				int uid = values.getAsInteger(COL_UID);
				boolean allowed = !Boolean.parseBoolean(values.getAsString(COL_RESTRICTED));

				// Get restrictions
				SharedPreferences prefs = getContext().getSharedPreferences(AUTHORITY, Context.MODE_PRIVATE);
				String restrictions = prefs.getString(getRestrictionPref(restrictionName), "*");

				// Decode restrictions
				List<String> listRestriction = new ArrayList<String>(Arrays.asList(restrictions.split(",")));
				boolean defaultAllowed = listRestriction.get(0).equals("*");

				// Allow or deny
				String sUid = Integer.toString(uid);
				if (defaultAllowed ? allowed : !allowed)
					listRestriction.remove(sUid);
				if (defaultAllowed ? !allowed : allowed)
					if (!listRestriction.contains(sUid))
						listRestriction.add(sUid);

				// Encode restrictions
				restrictions = TextUtils.join(",", listRestriction.toArray(new String[0]));

				// Update restriction
				SharedPreferences.Editor editor = prefs.edit();
				editor.putString(getRestrictionPref(restrictionName), restrictions);
				editor.commit();

				return 1; // rows
			} else
				throw new SecurityException();
		}
		throw new IllegalArgumentException();
	}

	private String getRestrictionPref(String restrictionName) {
		return COL_RESTRICTED + "." + restrictionName;
	}

	private String getUsagePref(int uid, String restrictionName) {
		return COL_LASTUSED + "." + uid + "." + restrictionName;
	}

	@Override
	public int delete(Uri url, String where, String[] selectionArgs) {
		throw new IllegalArgumentException();
	}
}